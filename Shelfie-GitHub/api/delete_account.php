<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';

$user_id = $_POST['user_id'] ?? '';
$currentPassword = $_POST['current_password'] ?? '';

if ($user_id === '' || $currentPassword === '') {
    echo json_encode([
        'success' => false,
        'message' => 'Missing user_id or password',
    ]);
    exit;
}

$userIdInt = (int) $user_id;
if ($userIdInt <= 0) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid user_id',
    ]);
    exit;
}

$sel = mysqli_prepare($con, 'SELECT id, password FROM users WHERE id = ? LIMIT 1');
mysqli_stmt_bind_param($sel, 'i', $userIdInt);
mysqli_stmt_execute($sel);
$res = mysqli_stmt_get_result($sel);
$user = $res ? $res->fetch_assoc() : null;
mysqli_stmt_close($sel);

if (!$user) {
    echo json_encode([
        'success' => false,
        'message' => 'User not found',
    ]);
    exit;
}

$storedHash = $user['password'] ?? '';
if (!password_verify($currentPassword, $storedHash)) {
    echo json_encode([
        'success' => false,
        'message' => 'Password is incorrect',
    ]);
    exit;
}

$stmt = mysqli_prepare($con, 'DELETE FROM users WHERE id = ?');
mysqli_stmt_bind_param($stmt, 'i', $userIdInt);
$ok = mysqli_stmt_execute($stmt);
mysqli_stmt_close($stmt);

if ($ok) {
    echo json_encode([
        'success' => true,
        'message' => 'Account deleted',
    ]);
} else {
    echo json_encode([
        'success' => false,
        'message' => mysqli_error($con),
    ]);
}
