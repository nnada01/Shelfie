<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
// __DIR__ keeps the include path stable regardless of how PHP was invoked

$userId = intval($_GET['user_id'] ?? 0);
if ($userId <= 0) {
    echo json_encode(['success' => false, 'message' => 'Invalid user']);
    exit;
}

// Fetch one user including reading goals and binary avatar blob
$stmt = $con->prepare("
    SELECT id, first_name, last_name, email, birthday, country, profile_image,
           reading_goal_type, reading_goal_value
    FROM users
    WHERE id = ?
    LIMIT 1
");
$stmt->bind_param("i", $userId);
$stmt->execute();
$result = $stmt->get_result();

if ($row = $result->fetch_assoc()) {
    $img64 = '';
    if (!is_null($row['profile_image'])) {
        $img64 = base64_encode($row['profile_image']);
    }

    echo json_encode([
        'success' => true,
        'user' => [
            'id' => (int)$row['id'],
            'first_name' => $row['first_name'] ?? '',
            'last_name' => $row['last_name'] ?? '',
            'email' => $row['email'] ?? '',
            'birthday' => $row['birthday'] ?? '',
            'country' => $row['country'] ?? '',
            'image_base64' => $img64,
            'reading_goal_type' => $row['reading_goal_type'] ?? '',
            'reading_goal_value' => isset($row['reading_goal_value']) ? (int)$row['reading_goal_value'] : 0
        ]
    ]);
} else {
    echo json_encode(['success' => false, 'message' => 'User not found']);
}
