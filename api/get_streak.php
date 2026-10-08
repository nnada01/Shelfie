<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
// __DIR__ keeps the include path stable regardless of how PHP was invoked

$user_id = $_GET['user_id'] ?? '';

if ($user_id === '') {
    echo json_encode(["success" => false, "message" => "Missing user_id"]);
    exit;
}

// Newest-first list of days with activity used to count consecutive days backward from today
$stmt = mysqli_prepare($con, "SELECT log_date FROM reading_logs WHERE user_id = ? ORDER BY log_date DESC");
mysqli_stmt_bind_param($stmt, "i", $user_id);
mysqli_stmt_execute($stmt);
$result = mysqli_stmt_get_result($stmt);

$dates = array();

while($row = mysqli_fetch_assoc($result))
{
    array_push($dates, $row['log_date']);
}

$streak= 0;
$current= strtotime(date("Y-m-d"));

// Advance streak while each expected calendar day matches the next log entry
for($i=0; $i<count($dates); $i++)
{
    $dbDate = strtotime($dates[$i]);
    if(date("Y-m-d", $dbDate) == date("Y-m-d", $current))
    {
        $streak++;
        $current = strtotime("-1 day", $current);
    }
    else{
        break;
    }
}

echo json_encode(array("streak"=>$streak));
?>
