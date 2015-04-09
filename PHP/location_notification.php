<?php

	$dsn = 'mysql:host=nearme-user.co7uoobllvol.us-east-1.rds.amazonaws.com;port=3306;dbname=nearme_user';
	$username = 'android_nearme';
	$password = 'CuT3d3ViL';

	$dbh = new PDO($dsn, $username, $password);
	$dbh->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);

	$sth = $dbh->prepare('SELECT * FROM user_info');
	$sth->execute();

	$rows = $sth->fetchAll();

	if (sizeof($rows))
	{
		foreach ($rows as $idx => $row) {
			$userid = $row['ID'];
			$totalnear = 0;

			date_default_timezone_set('America/Los_Angeles');
			$ctime = date("Y-m-d H:i:s");
			$ltime = $row['last_notification'];
			if ($ltime != NULL) {
				$ctimestamp = strtotime($ctime);
				$ltimestamp = strtotime($ltime);
				if( (abs($ctimestamp - $ltimestamp)/(60*60)) < 6)
				{
					continue;
				}
			}
			

			$rsth = $dbh->prepare('SELECT * FROM user_relations where source_id = '.$userid);
			$rsth->execute();

			$rrows = $rsth->fetchAll();

			foreach ($rrows as $indx => $rrow) {
				$tsth = $dbh->prepare('SELECT * FROM user_info where ID = '.$rrow['target_id']);
				$tsth->execute();

				$trows = $tsth->fetchAll();

				if (sizeof($trows)) {
					$sourceLat = $row['lat'];
					$sourceLon = $row['lon'];
					$targetLat = $trows[0]['lat'];
					$targetLon = $trows[0]['lon'];

					//TODO: calculate if the two are within some distance and then push gcm for notification to source
					//TODO: later on add settings to block users and depending on those settings send the notification to only those that are not blocked

					if(($sourceLat != NULL && $sourceLon != NULL && $targetLat != NULL && $targetLon != NULL) && distance($sourceLat, $sourceLon, $targetLat, $targetLon, 'K') < 1.0)
					{
						$totalnear++;
					}
				}
			}
			if($totalnear>0)
			{
				send_push_notification(array($row['gcm_regid']),"There are ".$totalnear." people you know near to you.");
				$sql = "UPDATE user_info SET last_notification='".$ctime."' WHERE ID = '".$userid."'";

				$q = $dbh->prepare($sql);
				$q->execute();
			}
		}
	}

	function distance($lat1, $lon1, $lat2, $lon2, $unit) {
	  $theta = $lon1 - $lon2;
	  $dist = sin(deg2rad($lat1)) * sin(deg2rad($lat2)) +  cos(deg2rad($lat1)) * cos(deg2rad($lat2)) * cos(deg2rad($theta));
	  $dist = acos($dist);
	  $dist = rad2deg($dist);
	  $miles = $dist * 60 * 1.1515;
	  $unit = strtoupper($unit);
	  if ($unit == "K") {
	    return ($miles * 1.609344);
	  } else if ($unit == "N") {
	      return ($miles * 0.8684);
	    } else {
	        return $miles;
	      }
	}

	 //Sending Push Notification
   function send_push_notification($registration_ids, $message) {
         
        // Set POST variables
        $url = 'https://android.googleapis.com/gcm/send';
 
        $fields = array(
            'registration_ids' => $registration_ids,
            'data' => array('message' => $message)
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
        echo $result;
    }


?>