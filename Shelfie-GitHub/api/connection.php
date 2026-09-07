<?php
// Database server login (adjust user/password/db to match your MySQL install)
$server="localhost";
$user="root";
$password="";
$db = "shelfie_db";

// Shared mysqli link included by every API script
$con = mysqli_connect($server,$user,$password,$db);

if(mysqli_connect_errno())
{
    echo mysqli_connect_error();
}
?>
