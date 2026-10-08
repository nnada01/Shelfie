<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
// __DIR__ keeps the include path stable regardless of how PHP was invoked

$id = $_GET['id'] ?? '';
$title = $_GET['title'] ?? '';
$content = $_GET['content'] ?? '';
$mood = $_GET['mood'] ?? '';
$entry_date = $_GET['entry_date'] ?? '';

if ($id === '' || $title === '' || $entry_date === '') {
    echo json_encode(["success" => false, "message" => "Missing required fields"]);
    exit;
}

// Overwrite title, body, mood, and date for the given journal primary key
$stmt = mysqli_prepare(
    $con,
    "UPDATE journals SET title = ?, content = ?, mood = ?, entry_date = ? WHERE id = ?"
);
mysqli_stmt_bind_param($stmt, "ssssi", $title, $content, $mood, $entry_date, $id);

if (mysqli_stmt_execute($stmt)) {
    echo json_encode(["success" => true, "message" => "Journal updated"]);
} else {
    echo json_encode(["success" => false, "message" => mysqli_error($con)]);
}
?>
