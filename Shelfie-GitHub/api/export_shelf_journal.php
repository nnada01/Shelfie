<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';

$userId = intval($_GET['user_id'] ?? 0);
if ($userId <= 0) {
    echo json_encode(['success' => false, 'message' => 'Invalid user']);
    exit;
}

// ISO timestamp so clients can label backup files
$exportedAt = date('c');

$books = [];
// Select every column for all books belonging to this user, ordered by id for stable exports
$stmt = $con->prepare('SELECT * FROM books WHERE user_id = ? ORDER BY id ASC');
$stmt->bind_param('i', $userId);
$stmt->execute();
$res = $stmt->get_result();
while ($row = $res->fetch_assoc()) {
    $books[] = [
        'id' => (int)$row['id'],
        'title' => $row['title'],
        'author' => $row['author'],
        'category' => $row['category'],
        'total_pages' => (int)$row['total_pages'],
        'current_page' => (int)$row['current_page'],
        'status' => $row['status'],
        'is_favorite' => (int)$row['is_favorite'],
        'rating' => (float)$row['rating'],
        'genres' => $row['genres'],
        'is_recommended' => (int)$row['is_recommended'],
        'completed_at' => $row['completed_at'],
    ];
}
$stmt->close();

$journals = [];
// Load journal rows for CSV/JSON backup sorted by entry date then id
$stmt = $con->prepare('SELECT id, title, content, mood, entry_date FROM journals WHERE user_id = ? ORDER BY entry_date ASC, id ASC');
$stmt->bind_param('i', $userId);
$stmt->execute();
$res = $stmt->get_result();
while ($row = $res->fetch_assoc()) {
    $journals[] = [
        'id' => (int)$row['id'],
        'title' => $row['title'],
        'content' => $row['content'],
        'mood' => $row['mood'],
        'entry_date' => $row['entry_date'],
    ];
}
$stmt->close();

echo json_encode([
    'success' => true,
    'exported_at' => $exportedAt,
    'user_id' => $userId,
    'books' => $books,
    'journals' => $journals,
]);
