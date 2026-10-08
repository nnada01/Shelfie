<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
require_once __DIR__ . '/weekly_pages.inc.php';

$id = $_GET['id'] ?? '';

if ($id === '') {
    echo json_encode(['success' => false, 'message' => 'Missing id']);
    exit;
}

$idInt = (int)$id;

$sel = mysqli_prepare($con, 'SELECT user_id FROM books WHERE id = ? LIMIT 1');
mysqli_stmt_bind_param($sel, 'i', $idInt);
mysqli_stmt_execute($sel);
$row = mysqli_stmt_get_result($sel)->fetch_assoc();
mysqli_stmt_close($sel);

if (!$row) {
    echo json_encode(['success' => false, 'message' => 'Book not found']);
    exit;
}

$userId = (int)$row['user_id'];
$weekKey = shelfie_week_key();

mysqli_begin_transaction($con);

$w = mysqli_prepare($con, 'DELETE FROM book_weekly_pages WHERE book_id = ?');
mysqli_stmt_bind_param($w, 'i', $idInt);
mysqli_stmt_execute($w);
mysqli_stmt_close($w);

$stmt = mysqli_prepare($con, 'DELETE FROM books WHERE id = ?');
mysqli_stmt_bind_param($stmt, 'i', $idInt);
$ok = mysqli_stmt_execute($stmt);
mysqli_stmt_close($stmt);

if ($ok) {
    shelfie_sync_user_week_pages($con, $userId, $weekKey);
    mysqli_commit($con);
    echo json_encode(['success' => true, 'message' => 'Book deleted']);
} else {
    mysqli_rollback($con);
    echo json_encode(['success' => false, 'message' => mysqli_error($con)]);
}
