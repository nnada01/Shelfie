<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
// __DIR__ keeps the include path stable regardless of how PHP was invoked

$first = trim($_POST['first_name'] ?? '');
$last = trim($_POST['last_name'] ?? '');
$email = trim($_POST['email'] ?? '');
$password = $_POST['password'] ?? '';

if ($first === '' || $last === '' || $email === '' || $password === '') {
    echo json_encode(['success' => false, 'message' => 'Missing required fields']);
    exit;
}

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    echo json_encode(['success' => false, 'message' => 'Invalid email address']);
    exit;
}

// Abort early if email is already taken
$check = $con->prepare("SELECT id FROM users WHERE email = ?");
$check->bind_param("s", $email);
$check->execute();
$check->store_result();
if ($check->num_rows > 0) {
    echo json_encode(['success' => false, 'message' => 'Email already exists']);
    exit;
}

$hashed = password_hash($password, PASSWORD_BCRYPT);

// Insert minimal user row; profile fields are filled later in the app
$stmt = $con->prepare("
    INSERT INTO users (first_name, last_name, email, password)
    VALUES (?, ?, ?, ?)
");
$stmt->bind_param("ssss", $first, $last, $email, $hashed);

if ($stmt->execute()) {
    echo json_encode(['success' => true, 'message' => 'Account created']);
} else {
    echo json_encode(['success' => false, 'message' => 'Failed to create account']);
}
