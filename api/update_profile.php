<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
// __DIR__ keeps the include path stable regardless of how PHP was invoked

// Writes reading_goal_* from POST when reading_goal_type key exists; returns an error string or null on success
function applyUserReadingExtras(mysqli $con, int $userId): ?string
{
    if (!array_key_exists('reading_goal_type', $_POST)) {
        return null;
    }

    $goalTypeRaw = trim((string)($_POST['reading_goal_type'] ?? ''));
    $goalValIn = (int)($_POST['reading_goal_value'] ?? 0);
    $goalType = null;
    $goalValueSql = null;
    if ($goalTypeRaw !== '' && strcasecmp($goalTypeRaw, 'none') !== 0) {
        if (!in_array($goalTypeRaw, ['pages_week', 'books_year'], true)) {
            return 'Invalid reading goal type.';
        }
        if ($goalValIn <= 0) {
            return 'Reading goal value must be a positive number.';
        }
        $goalType = $goalTypeRaw;
        $goalValueSql = $goalValIn;
    }

    if ($goalType === null) {
        $stmt = $con->prepare('UPDATE users SET reading_goal_type = NULL, reading_goal_value = NULL WHERE id = ?');
        $stmt->bind_param('i', $userId);
    } else {
        $stmt = $con->prepare('UPDATE users SET reading_goal_type = ?, reading_goal_value = ? WHERE id = ?');
        $stmt->bind_param('sii', $goalType, $goalValueSql, $userId);
    }
    $stmt->execute();
    $stmt->close();

    return null;
}

$userId = intval($_POST['user_id'] ?? 0);
$first = trim($_POST['first_name'] ?? '');
$last = trim($_POST['last_name'] ?? '');
$birthday = trim($_POST['birthday'] ?? '');
$country = trim($_POST['country'] ?? '');
$imageBase64 = trim($_POST['image_base64'] ?? '');
$removeImage = isset($_POST['remove_profile_image']) && $_POST['remove_profile_image'] === '1';

if ($userId <= 0 || $first === '' || $last === '') {
    echo json_encode(['success' => false, 'message' => 'Invalid profile data']);
    exit;
}

if ($removeImage) {
    // Names, dates, country, and force profile_image to NULL (empty image_base64 alone means leave photo unchanged)
    $stmt = $con->prepare("
        UPDATE users
        SET first_name = ?, last_name = ?, birthday = NULLIF(?, ''), country = NULLIF(?, ''), profile_image = NULL
        WHERE id = ?
    ");
    $stmt->bind_param("ssssi", $first, $last, $birthday, $country, $userId);
} elseif ($imageBase64 !== '') {
    $imageBinary = base64_decode($imageBase64);
    if ($imageBinary === false) {
        echo json_encode(['success' => false, 'message' => 'Invalid image encoding']);
        exit;
    }

    // Replace BLOB column with newly uploaded bytes
    $stmt = $con->prepare("
        UPDATE users
        SET first_name = ?, last_name = ?, birthday = NULLIF(?, ''), country = NULLIF(?, ''), profile_image = ?
        WHERE id = ?
    ");
    $stmt->bind_param("sssssi", $first, $last, $birthday, $country, $imageBinary, $userId);
} else {
    // Touch text fields only so existing profile_image row stays as-is
    $stmt = $con->prepare("
        UPDATE users
        SET first_name = ?, last_name = ?, birthday = NULLIF(?, ''), country = NULLIF(?, '')
        WHERE id = ?
    ");
    $stmt->bind_param("ssssi", $first, $last, $birthday, $country, $userId);
}

if ($stmt->execute()) {
    $extraErr = applyUserReadingExtras($con, $userId);
    if ($extraErr !== null) {
        echo json_encode(['success' => false, 'message' => $extraErr]);
        exit;
    }
    echo json_encode(['success' => true, 'message' => 'Profile updated']);
} else {
    echo json_encode(['success' => false, 'message' => 'Update failed: ' . $stmt->error]);
}
