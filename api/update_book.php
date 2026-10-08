<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
require_once __DIR__ . '/weekly_pages.inc.php';

$id = $_GET['id'] ?? '';
$title = $_GET['title'] ?? '';
$author = $_GET['author'] ?? '';
$category = $_GET['category'] ?? '';
$total_pages = $_GET['total_pages'] ?? '0';
$current_page = $_GET['current_page'] ?? '0';
$status = $_GET['status'] ?? 'Reading';
$is_favorite = $_GET['is_favorite'] ?? '0';
$rating = $_GET['rating'] ?? '0';
$genres = $_GET['genres'] ?? '';
$is_recommended = $_GET['is_recommended'] ?? '0';

if ($id === '' || $title === '' || $author === '') {
    echo json_encode(['success' => false, 'message' => 'Missing required fields']);
    exit;
}

$statusNorm = strtolower(trim((string)$status));
$isFinished = in_array($statusNorm, ['completed', 'finished'], true);

$idInt = (int)$id;
$newCp = (int)$current_page;
$newTp = (int)$total_pages;

$sel = mysqli_prepare($con, 'SELECT user_id, current_page, total_pages FROM books WHERE id = ? LIMIT 1');
mysqli_stmt_bind_param($sel, 'i', $idInt);
mysqli_stmt_execute($sel);
$prev = mysqli_stmt_get_result($sel)->fetch_assoc();
mysqli_stmt_close($sel);

if (!$prev) {
    echo json_encode(['success' => false, 'message' => 'Book not found']);
    exit;
}

$bookUserId = (int)$prev['user_id'];
$oldEff = shelfie_effective_pages((int)$prev['current_page'], (int)$prev['total_pages']);
$newEff = shelfie_effective_pages($newCp, $newTp);
$signedDelta = $newEff - $oldEff;

// Update every editable column on the book; set completed_at once when first marked finished (COALESCE keeps an existing date)
$stmt = mysqli_prepare($con, 'UPDATE books SET
title=?, author=?, category=?, total_pages=?, current_page=?, status=?, is_favorite=?, rating=?, genres=?, is_recommended=?,
completed_at = IF(?, COALESCE(completed_at, CURDATE()), NULL)
WHERE id=?');

$finishedFlag = $isFinished ? 1 : 0;

mysqli_stmt_bind_param(
    $stmt,
    'sssiisidsiii',
    $title,
    $author,
    $category,
    $total_pages,
    $current_page,
    $status,
    $is_favorite,
    $rating,
    $genres,
    $is_recommended,
    $finishedFlag,
    $idInt
);

mysqli_begin_transaction($con);
if (!mysqli_stmt_execute($stmt)) {
    mysqli_rollback($con);
    mysqli_stmt_close($stmt);
    echo json_encode(['success' => false, 'message' => mysqli_error($con)]);
    exit;
}
mysqli_stmt_close($stmt);

if ($signedDelta !== 0) {
    $weekKey = shelfie_week_key();
    shelfie_apply_weekly_book_delta($con, $bookUserId, $idInt, $weekKey, $signedDelta);
    shelfie_sync_user_week_pages($con, $bookUserId, $weekKey);
}

mysqli_commit($con);

echo json_encode(['success' => true, 'message' => 'Book updated']);
