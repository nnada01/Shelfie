<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
// __DIR__ keeps the include path stable regardless of how PHP was invoked

$user_id = $_GET['user_id'] ?? '';
$title = $_GET['title'] ?? '';
$content = $_GET['content'] ?? '';
$mood = $_GET['mood'] ?? '';
$entry_date = $_GET['entry_date'] ?? '';

if ($user_id === '' || $title === '' || $entry_date === '') {
    echo json_encode(["success" => false, "message" => "Missing required fields"]);
    exit;
}

// Append one journal entry linked to user_id
$stmt = mysqli_prepare($con, "INSERT INTO journals (user_id, title, content, mood, entry_date) VALUES (?, ?, ?, ?, ?)"
);
mysqli_stmt_bind_param($stmt, "issss", $user_id, $title, $content, $mood, $entry_date);

if (mysqli_stmt_execute($stmt)) {
    echo json_encode(["success" => true, "message" => "Journal added"]);
} else {
    echo json_encode(["success" => false, "message" => mysqli_error($con)]);
}
?>
