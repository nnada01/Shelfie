<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
// __DIR__ keeps the include path stable regardless of how PHP was invoked

$id = $_GET['id'] ?? '';
$fav = $_GET['fav'] ?? '';

if ($id === '' || ($fav !== '0' && $fav !== '1')) {
    echo json_encode(["success" => false, "message" => "Invalid input"]);
    exit;
}

// Set is_favorite to 0 or 1 for one book id
$stmt = mysqli_prepare($con, "UPDATE books SET is_favorite = ? WHERE id = ?");
mysqli_stmt_bind_param($stmt, "ii", $fav, $id);

if (mysqli_stmt_execute($stmt)) {
    echo json_encode(["success" => true, "message" => "Favorite updated"]);
} else {
    echo json_encode(["success" => false, "message" => mysqli_error($con)]);
}
?>
