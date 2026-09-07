<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
// __DIR__ keeps the include path stable regardless of how PHP was invoked

$user_id = $_GET['user_id'] ?? '';

if ($user_id === '') {
    echo json_encode(["success" => false, "message" => "Missing user_id"]);
    exit;
}

// All journal rows for this user with newest database id first
$stmt = mysqli_prepare($con, "SELECT * FROM journals WHERE user_id = ? ORDER BY id DESC");
mysqli_stmt_bind_param($stmt, "i", $user_id);
mysqli_stmt_execute($stmt);
$result = mysqli_stmt_get_result($stmt);

$return_array = array();
$journals_array = array();

while($row = mysqli_fetch_assoc($result))
{
    $row_array = array();
    $row_array['id'] = $row['id'];
    $row_array['title'] = $row['title'];
    $row_array['content'] = $row['content'];
    $row_array['mood'] = $row['mood'];
    $row_array['entry_date'] = $row['entry_date'];

    array_push($journals_array, $row_array);
}

$return_array['journals'] = $journals_array;

echo json_encode($return_array);
?>
