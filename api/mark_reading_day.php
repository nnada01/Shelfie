<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
// __DIR__ keeps the include path stable regardless of how PHP was invoked

$user_id = $_GET['user_id'] ?? '';
$today = date("Y-m-d");

if ($user_id === '') {
    echo json_encode(["success" => false, "message" => "Missing user_id"]);
    exit;
}

// See if today is already logged so we never insert duplicate (user_id, log_date) pairs
$checkStmt = mysqli_prepare($con, "SELECT id FROM reading_logs WHERE user_id = ? AND log_date = ?");
mysqli_stmt_bind_param($checkStmt, "is", $user_id, $today);
mysqli_stmt_execute($checkStmt);
$result = mysqli_stmt_get_result($checkStmt);

if (mysqli_num_rows($result) === 0) {
    // First open of the app today: record one row for streak tracking
    $insertStmt = mysqli_prepare($con, "INSERT INTO reading_logs (user_id, log_date) VALUES (?, ?)");
    mysqli_stmt_bind_param($insertStmt, "is", $user_id, $today);
    mysqli_stmt_execute($insertStmt);
}

echo json_encode(["success" => true, "message" => "Reading day marked"]);
?>
