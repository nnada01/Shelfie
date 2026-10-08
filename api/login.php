<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
// __DIR__ keeps the include path stable regardless of how PHP was invoked

$email = trim($_POST['email'] ?? '');
$password = $_POST['password'] ?? '';

if ($email === '' || $password === '') {
    echo json_encode([
        'success' => false,
        'message' => 'Email and password are required'
    ]);
    exit;
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid email address'
    ]);
    exit;
}

// Load credentials and display names for one email (password checked in PHP)
$stmt = $con->prepare("
    SELECT id, first_name, last_name, email, password, is_admin
    FROM users
    WHERE email = ?
    LIMIT 1
");
$stmt->bind_param("s", $email);
$stmt->execute();
$result = $stmt->get_result();

if (!$result || $result->num_rows === 0) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid email or password'
    ]);
    exit;
}

$user = $result->fetch_assoc();
$storedHash = $user['password'] ?? '';

// Compare plaintext POST password to bcrypt hash from signup
if (!password_verify($password, $storedHash)) {
    echo json_encode([
        'success' => false,
        'message' => 'Invalid email or password'
    ]);
    exit;
}

// Transparently upgrade hash cost if PHP’s default bcrypt options changed
if (password_needs_rehash($storedHash, PASSWORD_BCRYPT)) {
    $newHash = password_hash($password, PASSWORD_BCRYPT);
    $upd = $con->prepare("UPDATE users SET password = ? WHERE id = ?");
    $upd->bind_param("si", $newHash, $user['id']);
    $upd->execute();
}

$first = $user['first_name'] ?? '';
$last = $user['last_name'] ?? '';

echo json_encode([
    'success' => true,
    'message' => 'Login successful',
    'user' => [
        'id' => (int)$user['id'],
        'first_name' => $first,
        'last_name' => $last,
        'email' => $user['email'] ?? '',
        'is_admin' => (int)($user['is_admin'] ?? 0)
    ]
]);
