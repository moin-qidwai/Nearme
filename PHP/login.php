<?php
	require_once 'countries.php';

	$dsn = 'mysql:host=nearme-user.co7uoobllvol.us-east-1.rds.amazonaws.com;port=3306;dbname=nearme_user';
	$username = 'android_nearme';
	$password = 'CuT3d3ViL';

	$dbh = new PDO($dsn, $username, $password);
	$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

	$_POST = json_decode(file_get_contents("php://input"));
	$username = $_POST->username;

	$rsth = $dbh->prepare('SELECT * FROM user_info WHERE name = "'.$username.'"');
	$rsth->execute();

	$rrows = $rsth->fetchAll();

	if (sizeof($rrows))
	{
		echo '{ status : 1, "ID" : '.$rrows[0]['ID'].', "number" : "'.$rrows[0]['number'].'", "name" : "'.$rrows[0]['name'].'", "countryCode" : "'.$rrows[0]['country'].'", "displayName" : "'.$rows[0]['display_name'].'" }';
	}
	else
	{
		echo '{ status : 0, "message" : "The entered username does not exist. Please try again or register." }';
	}

?> 		