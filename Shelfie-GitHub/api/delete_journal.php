<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
// __DIR__ keeps the include path stable regardless of how PHP was invoked

$id = $_GET['id'] ?? '';

if ($id === '') {
    echo json_encode(["success" => false, "message" => "Missing id"]);
    exit;
}

// Delete a single journal entry by id
$stmt = mysqli_prepare($con, "DELETE FROM journals WHERE id = ?");
mysqli_stmt_bind_param($stmt, "i", $id);

if (mysqli_stmt_execute($stmt)) {
    echo json_encode(["success" => true, "message" => "Journal deleted"]);
} else {
    echo json_encode(["success" => false, "message" => mysqli_error($con)]);
}
?>
