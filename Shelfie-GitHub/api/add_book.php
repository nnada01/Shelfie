<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
require_once __DIR__ . '/weekly_pages.inc.php';

// Incoming fields from GET query parameters (Android Volley uses this URL)
$user_id = $_GET['user_id'] ?? '';
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

if ($user_id === '' || $title === '' || $author === '') {
    echo json_encode(['success' => false, 'message' => 'Missing required fields']);
    exit;
}

// normalize status so we can set completed_at only for finished books
$statusNorm = strtolower(trim((string)$status));
$isFinished = in_array($statusNorm, ['completed', 'finished'], true) ? 1 : 0;

// insert one row into books; completed_at is today when the book is finished, otherwise NULL
$stmt = mysqli_prepare($con, 'INSERT INTO books
(user_id, title, author, category, total_pages, current_page, status, is_favorite, rating, genres, is_recommended, completed_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, IF(?, CURDATE(), NULL))');

mysqli_stmt_bind_param(
    $stmt,
    'isssiisidsii',
    $user_id,
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
    $isFinished
);

if (mysqli_stmt_execute($stmt)) {
    $newId = mysqli_insert_id($con);
    mysqli_stmt_close($stmt);
    $userIdInt = (int)$user_id;
    $eff = shelfie_effective_pages((int)$current_page, (int)$total_pages);
    if ($eff > 0) {
        $weekKey = shelfie_week_key();
        $ins = mysqli_prepare($con, 'INSERT INTO book_weekly_pages (user_id, book_id, week_key, pages) VALUES (?, ?, ?, ?)');
        mysqli_stmt_bind_param($ins, 'iisi', $userIdInt, $newId, $weekKey, $eff);
        mysqli_stmt_execute($ins);
        mysqli_stmt_close($ins);
        shelfie_sync_user_week_pages($con, $userIdInt, $weekKey);
    }
    echo json_encode(['success' => true, 'message' => 'Book added', 'id' => $newId]);
} else {
    mysqli_stmt_close($stmt);
    echo json_encode(['success' => false, 'message' => mysqli_error($con)]);
}
