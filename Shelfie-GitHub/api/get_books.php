<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
// __DIR__ keeps the include path stable regardless of how PHP was invoked

$user_id = $_GET['user_id'] ?? '';

if ($user_id === '') {
    echo json_encode(["success" => false, "message" => "Missing user_id"]);
    exit;
}

// Full book list for the shelf screen (recent ids first)
$stmt = mysqli_prepare($con, "SELECT * FROM books WHERE user_id = ? ORDER BY id DESC");
mysqli_stmt_bind_param($stmt, "i", $user_id);
mysqli_stmt_execute($stmt);
$result = mysqli_stmt_get_result($stmt);

$return_array = array();
$books_array = array();

while ($row = mysqli_fetch_assoc($result)) {
    $row_array = array();

    $row_array['id'] = (int)$row['id'];
    $row_array['title'] = $row['title'];
    $row_array['author'] = $row['author'];
    $row_array['category'] = $row['category'];
    $row_array['total_pages'] = (int)$row['total_pages'];
    $row_array['current_page'] = (int)$row['current_page'];
    $row_array['status'] = $row['status'];
    $row_array['is_favorite'] = (int)$row['is_favorite'];

    $row_array['rating'] = (float)$row['rating'];
    $row_array['genres'] = $row['genres'];
    $row_array['is_recommended'] = (int)$row['is_recommended'];

    $books_array[] = $row_array;
}

$return_array['books'] = $books_array;

echo json_encode($return_array);
?>
