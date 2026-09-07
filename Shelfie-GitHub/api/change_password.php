<?php

//change password for a logged-in user (POST: user_id, current_password, new_password)

header('Content-Type: application/json; charset=UTF-8');
require_once __DIR__ . '/connection.php';

$userId = isset($_POST['user_id']) ? (int)$_POST['user_id'] : 0;
$current = $_POST['current_password'] ?? '';
$newPlain = $_POST['new_password'] ?? '';

if ($userId <= 0 || $current === '' || $newPlain === '') {
    echo json_encode([
        'success' => false,
        'message' => 'Missing user_id, current password, or new password',
    ]);
    exit;
}

/** Same rules as Android PasswordRules (length, upper, lower, special). */
function shelfie_password_valid(string $password): bool
{
    if (strlen($password) < 8) {
        return false;
    }
    $hasUpper = false;
    $hasLower = false;
    $hasSpecial = false;
    $len = strlen($password);
    for ($i = 0; $i < $len; $i++) {
        $c = $password[$i];
        if (ctype_alpha($c)) {
            if (ctype_upper($c)) {
                $hasUpper = true;
            }
            if (ctype_lower($c)) {
                $hasLower = true;
            }
        } elseif (!ctype_digit($c) && $c !== ' ' && $c !== "\t" && $c !== "\n" && $c !== "\r") {
            $hasSpecial = true;
        }
    }
    return $hasUpper && $hasLower && $hasSpecial;
}

if (!shelfie_password_valid($newPlain)) {
    echo json_encode([
        'success' => false,
        'message' => 'New password does not meet requirements (8+ chars, upper, lower, special)',
    ]);
    exit;
}



$stmt = $con->prepare('SELECT id, password FROM users WHERE id = ? LIMIT 1');
if (!$stmt) {
    echo json_encode(['success' => false, 'message' => 'Server error']);
    exit;
}

$stmt->bind_param('i', $userId);
$stmt->execute();
$result = $stmt->get_result();
$user = $result ? $result->fetch_assoc() : null;
$stmt->close();

if (!$user) {
    echo json_encode(['success' => false, 'message' => 'User not found']);
    exit;
}

$storedHash = $user['password'] ?? '';
if (!password_verify($current, $storedHash)) {
    echo json_encode(['success' => false, 'message' => 'Current password is incorrect']);
    exit;
}

if (password_verify($newPlain, $storedHash)) {
    echo json_encode(['success' => false, 'message' => 'New password must be different from the current one']);
    exit;
}

$newHash = password_hash($newPlain, PASSWORD_BCRYPT);
$upd = $con->prepare('UPDATE users SET password = ? WHERE id = ?');
if (!$upd) {
    echo json_encode(['success' => false, 'message' => 'Server error']);
    exit;
}

$upd->bind_param('si', $newHash, $userId);
if ($upd->execute()) {
    echo json_encode([
        'success' => true,
        'message' => 'Password updated',
    ]);
} else {
    echo json_encode([
        'success' => false,
        'message' => 'Could not update password',
    ]);
}
$upd->close();
