<?php
	require_once 'countries.php';

	$dsn = 'mysql:host=nearme-user.co7uoobllvol.us-east-1.rds.amazonaws.com;port=3306;dbname=nearme_user';
	$username = 'android_nearme';
	$password = 'CuT3d3ViL';

	$dbh = new PDO($dsn, $username, $password);
	$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

	$_POST = json_decode(file_get_contents("php://input"));
	$usernum = $_POST->usernum;
	$username = $_POST->username;
	$countryCode = $_POST->countryCode;
	$displayName = $_POST->displayName;

	$usernum = '+'.$extensions[$countryCode].$usernum;
	try
	{
		$sth = $dbh->prepare('SELECT * FROM user_info WHERE number = '.$usernum);
		$sth->execute();
	}
	catch(Exception $e)
	{
		echo $e->getMessage();
	}

	$rows = $sth->fetchAll();

	try
	{
		$rsth = $dbh->prepare('SELECT * FROM user_info WHERE name = "'.$username.'"');
		$rsth->execute();
	}
	catch(Exception $e)
	{
		echo $e->getMessage();
	}
	
	$rrows = $rsth->fetchAll();

	if(sizeof($rows))
	{
		// Return the user record to the application
		echo '{ status : 0, error : 0, "message" : "The entered phone number already exists. Please login using your username." }';
	}
	elseif (sizeof($rrows))
	{
		echo '{ status : 0, error : 1, "message" : "The entered username already exists. Please login using your username." }';
	}
	else
	{
		// new data
		$name = $username;
		$number = $usernum;
		$code = $countryCode;
		$dName = $displayName;

		// Insert a new user record.

		$sql = "INSERT INTO user_info (name,number,country,display_name) VALUES (:name,:number,:country,:display_name)";

		$q = $dbh->prepare($sql);
		$q->execute(array(':name'=>$name,
		                  ':number'=>$number,
		                  ':country'=>$code,
		                  ':display_name'=>$dName));

		// Get the newly inserted record and return it to the application.

		$sth = $dbh->prepare('SELECT * FROM user_info WHERE number = '.$usernum);
		$sth->execute();

		$rows = $sth->fetchAll();
		echo '{ status : 1, "ID" : '.$rows[0]['ID'].', "number" : "'.$rows[0]['number'].'", "name" : "'.$rows[0]['name'].'", "countryCode" : "'.$rows[0]['country'].'", "displayName" : "'.$rows[0]['display_name'].'" }';

	}

?> 		