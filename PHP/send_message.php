<?php

	$dsn = 'mysql:host=nearme-user.co7uoobllvol.us-east-1.rds.amazonaws.com;port=3306;dbname=nearme_user';
	$username = 'android_nearme';
	$password = 'CuT3d3ViL';

	$dbh = new PDO($dsn, $username, $password);
	$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

	$_POST = json_decode(file_get_contents("php://input"));
	$sourceId = $_POST->sourceId;
	$targetId = $_POST->targetId;
	$message = $_POST->message;

	try
	{
		$rsth = $dbh->prepare('SELECT * FROM user_info WHERE ID = '.$sourceId);
		$rsth->execute();
	}
	catch(Exception $e)
	{
		echo $e->getMessage();
	}

	$rrows = $rsth->fetchAll();

	try
	{
		$sth = $dbh->prepare('SELECT * FROM user_info WHERE ID = '.$targetId);
		$sth->execute();
	}
	catch(Exception $e)
	{
		echo $e->getMessage();
	}

	$rows = $sth->fetchAll();

	if(sizeof($rrows) && sizeof($rows))
	{
		send_push_notification($rrows[0]['display_name'], array($rows[0]['gcm_regid']),$message);
	}

	

 //Sending Push Notification
   function send_push_notification($fromuser ,$registration_ids, $message) {
         
        // Set POST variables
        $url = 'https://android.googleapis.com/gcm/send';
 
        $fields = array(
            'registration_ids' => $registration_ids,
            'data' => array('type' => "message", "message" => $message, "fromuser" => $fromuser)
        );
 
        $headers = array(
            'Authorization: key=' . 'AIzaSyBXnz9iuscK8JpVmtCLHoD_j6ZIOzvIzo8',
            'Content-Type: application/json'
        );
        //print_r($headers);
        // Open connection
        $ch = curl_init();
 
        // Set the url, number of POST vars, POST data
        curl_setopt($ch, CURLOPT_URL, $url);
 
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
 
        // Disabling SSL Certificate support temporarly
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
 
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
 
        // Execute post
        $result = curl_exec($ch);
        if ($result === FALSE) {
            die('Curl failed: ' . curl_error($ch));
        }
 
        // Close connection
        curl_close($ch);
        echo "{ result: success }";
    }