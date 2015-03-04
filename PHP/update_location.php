<?php
	require_once 'countries.php';

	$dsn = 'mysql:host=nearme-user.co7uoobllvol.us-east-1.rds.amazonaws.com;port=3306;dbname=nearme_user';
	$username = 'android_nearme';
	$password = 'CuT3d3ViL';

	try {
		$dbh = new PDO($dsn, $username, $password);
	}
	catch(Exception $e)
	{
		echo $e->getMessage();
	}
	
	$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

	$_POST = json_decode(file_get_contents("php://input"));
	$sourceId = $_POST->sourceId;
	$latitude = $_POST->lat;
	$longitude = $_POST->lon;

	$sql = "UPDATE user_info SET lat=".$latitude.", lon=".$longitude." WHERE ID = ".$sourceId;

	$q = $dbh->prepare($sql);
	$q->execute();

	echo '{ "status" : 1 }';

	?>