import time
import BaseHTTPServer
from urlparse import urlparse
from gcm import *
import socket
import MySQLdb
from math import sqrt, atan, degrees, pi, asin

HOST_NAME = '10.10.100.184' # !!!REMEMBER TO CHANGE THIS!!!
PORT_NUMBER = 8080

PIP = "10.10.100.191" #Pedestrian's IP address
PORT = 8888
MESSAGE = "Hello, World!"

db = MySQLdb.connect('localhost','root','123qwe','CASS')

cur = db.cursor()
query = "DELETE FROM `Car` WHERE 1"
cur.execute(query)
db.commit()
cur = db.cursor()
query = "DELETE FROM `Driver` WHERE 1"
cur.execute(query)
db.commit()
cur = db.cursor()
query = "DELETE FROM `Pedestrian` WHERE 1"
cur.execute(query)
db.commit()

class MyHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_HEAD(s):
        s.send_response(200)
        s.send_header("Content-type", "text/html")
        s.end_headers()
    def do_GET(s):
	global db
        """Respond to a GET request."""
        s.send_response(200)
        s.send_header("Content-type", "text/html")
        s.end_headers()
        #s.wfile.write("<html><head><title>Car Accident Safty System</title></head>")
        #s.wfile.write("<body><p>This is a test.</p>")
        # If someone went to "http://something.somewhere.net/foo/bar/",
        # then s.path equals "/foo/bar/".
	ind = s.path.find('?')
	fname = s.path[1:ind]
        #s.wfile.write("<p>fname = %s</p>" % fname)
	if fname == 'c.js':#Car
		query = urlparse(s.path).query
		query_components = dict(qc.split("=") for qc in query.split("&"))
		uuid = query_components["uuid"]
		velocity = query_components["velocity"]
		ss = query_components["ss"]
		direction = query_components["direction"]
		roadType = query_components["roadType"]
		emergency = query_components["emer"]
		if emergency == 'y':
			data = {'the_message': 'Emergency!', 'param2': 'value2'}
			s.gcmMessage('driver',data)
			s.gcmMessage('pedestrian',data)
		if ss == 'y' and roadType == 'N':#National Highway
			query = 'select DISTINCT uuid from Driver order by timestamp desc'
			cur.execute(query);
			results = cur.fetchall()
			#Send an warning to the other drivers if the distance is smaller than 1km
			s.gcmMessage('driver',data)
		print 'Car Information'
        	print "\tuuid : %s, velocity : %s, ss = %c, direction = %s, roadType = %c, emergency = %c" % (uuid,velocity,ss,direction,roadType,emergency)
		cur = db.cursor()
		query = "INSERT INTO Car VALUES (CURRENT_TIMESTAMP, \'%s\', %s, \'%c\', %s, \'%c\');" % (uuid,velocity,ss,direction,roadType) 
		cur.execute(query)
		db.commit()

	elif fname == 'd.js':#Driver
		query = urlparse(s.path).query
		query_components = dict(qc.split("=") for qc in query.split("&"))
		uuid = query_components["uuid"]
		latitude = query_components["latitude"]
		longitude = query_components["longitude"]
		heartbeat = query_components["heartbeat"]
		pn = query_components["phonenumber"]
        	#s.wfile.write("<p>uuid : %s, latitude : %s, longitude = %s, heartbeat = %s, pn = %s</p>" % (uuid,latitude,longitude,heartbeat,pn))
		print 'Driver Information'
        	print "\tuuid : %s, latitude : %s, longitude = %s, heartbeat = %s, pn = %s" % (uuid,latitude,longitude,heartbeat,pn)
		cur = db.cursor()
		query = "INSERT INTO Driver VALUES (CURRENT_TIMESTAMP, %s, %s, %s, %s, %s);" % (uuid,pn,latitude,longitude,heartbeat) 
		cur.execute(query)
		db.commit()

	elif fname == 'p.js':#Pedestrian
		query = urlparse(s.path).query
		query_components = dict(qc.split("=") for qc in query.split("&"))
		uuid = query_components["uuid"]
		latitude = query_components["latitude"]
		#latitude = '37.5145985'#Bongeunsa
		latitude = '37.514578'#Crossroad
		longitude = query_components["longitude"]
		#longitude = '127.0636795'#Bongeunsa
		longitude = '127.063533'#Crossroad
		heartbeat = query_components["heartbeat"]
		pn = query_components["phonenumber"]
        	#s.wfile.write("<p>uuid : %s, latitude : %s, longitude = %s, heartbeat = %s, pn = %s</p>" % (uuid,latitude,longitude,heartbeat,pn))
		print 'Pedestrian Information'
        	print "\tuuid : %s, latitude : %s, longitude = %s, heartbeat = %s, pn = %s" % (uuid,latitude,longitude,heartbeat,pn)
		cur = db.cursor()
		query = "INSERT INTO Pedestrian VALUES (CURRENT_TIMESTAMP ,%s, %s, %s, %s, %s);" % (uuid,pn,latitude,longitude,heartbeat) 
		cur.execute(query)
		db.commit()
		#Analyze
		query = 'select * from Driver where uuid = %s order by timestamp desc' % (uuid)
		cur.execute(query);
		result = cur.fetchone()
		degree = 0
		sendDegree = 0
		if result is not None:
			carY = float(result[3])
			carX = float(result[4])
			print "\tCar's position = %s, %s" % (result[3],result[4])
			pY = float(latitude)
			pX = float(longitude)
			if carY==pY:
				if carX > pX:
					angle = pi/2
				else:
					angle = -pi/2
			else:
				angle = atan((carX-pX)/(pY-carY))
			degree = (degrees(angle)+180)%180
			if carX < pX:
				degree += 180
			print '\tDegree = ', degree
		query = 'select * from Car where uuid = %s order by timestamp desc' % (uuid)
		cur.execute(query);
		results = cur.fetchmany(3)
		sumV = 0
		for row in results:
   	 		sumV += row[2] #Count the stopped car
		if sumV!=0:#The car is moving
			print "\tCar is moving"
			query = 'select * from Car where uuid = %s order by timestamp desc' % (uuid)
			cur.execute(query);
			results = cur.fetchmany(5)
			countType = 0
			for row in results:
				if row[5]=='U':#Normal Road
   	 				countType += 1
			if countType==5:#Normal Road
				print "\tNormal Road"
				#calculate the distance between car and pedestrian and send an warning to pedestrian if they become close
				query = 'select * from Driver where uuid = %s order by timestamp desc' % (uuid)
				cur.execute(query);
				results = cur.fetchmany(5)
				cDistance = 0
				count = 0
				row = results[0]
				pDistance = s.calculateDistance(float(row[3]),float(row[4]),float(latitude),float(longitude))
				for row in results[1:]:
					cDistance = s.calculateDistance(float(row[3]),float(row[4]),float(latitude),float(longitude))
					#print '\tDistance = ', cDistance
					if cDistance > pDistance:
   	 					count += 1
					pDistance = cDistance
				print '\tCount = ', count
				if count>=3:
					print '\tCar becomes close'
					query = 'select * from Car where uuid = %s order by timestamp desc' % (uuid)
					cur.execute(query);
					results = cur.fetchmany(5)
					suddenStop = 0
					for row in results:
						if row[3] == 'y':
							suddenStop = 1
							print'\tSudden Stop!'
					if suddenStop==1:
						data = {'the_message': 'Are you okay?', 'param2': 'value2'}
						s.gcmMessage('driver',data)
						s.gcmMessage('pedestrian',data)
					else:
						query = 'select * from Driver where uuid = %s order by timestamp desc' % (uuid)
						cur.execute(query);
						results = cur.fetchmany(2)
						if len(results)==2:
							pX = longitude
							pY = latitude
							cX = results[0][4]
							cY = results[0][3]
							cpX = results[1][4]
							cpY = results[1][3]
							X1 = cX - cpX
							Y1 = cY - cpY
							X2 = pX - cpX
							Y2 = pY - cpY
							tAngle = asin( (X1*Y2-Y1*X2) / (s.calculateDistance(0,0,X1,Y1) * s.calculateDistance(0,0,X2,Y2) ) )
							tDegree = degrees(tAngle)
							print '\ttDegree = %s' % (tDegree)		
						print '\tBeep!'
						sendDegree = 1
						sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
						sock.sendto(str(int(-degree)), (PIP, PORT))
		if sendDegree == 0:
			sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
			sock.sendto(str(int(degree)), (PIP, PORT))

        #s.wfile.write("<p>You accessed path: %s</p>" % s.path)
        #s.wfile.write("</body></html>")

    def gcmMessage(self, cn,data):
    	if cn=='driver':
		gcm = GCM("AIzaSyBOWTgS_EXoaXgFoEgVEk4Pct3DdMJyFUA")
		reg_id = 'APA91bFQUXg_ZObnd-SfW22yLyNFBCsD4p0IQ5dEx0k5ALHfmWwzjYshDoubJWG4G6pZqximy2MUgY-s7J8UT4M_727NsVauQUBG5slW5E8w84xGHhzvTxlxsEu098RLtJGI2NWZa6E8'
		gcm.plaintext_request(registration_id=reg_id, data=data)
	elif cn=='pedestrian':
		gcm = GCM("AIzaSyBOWTgS_EXoaXgFoEgVEk4Pct3DdMJyFUA")
		reg_id = 'APA91bFQD9C8XMib-4XYlhM7eIh75qpDWlypoPTLza0Pjt9XK3dpSel28IwLs5wpqW4S6_5lqFak_MhTxdwcnUosRFo3Z5z3bb5M5Riq5OfR9QOR2mP53gDeGmCSZ9JU4YWjCOmDBOUN'
		gcm.plaintext_request(registration_id=reg_id, data=data)

    def calculateDistance(self, cX, cY, pX, pY):
	return sqrt( (cX - pX)**2 + (cY - pY)**2 )

if __name__ == '__main__':
    server_class = BaseHTTPServer.HTTPServer
    httpd = server_class((HOST_NAME, PORT_NUMBER), MyHandler)
    print time.asctime(), "Server Starts - %s:%s" % (HOST_NAME, PORT_NUMBER)
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        pass
    httpd.server_close()
    db.close()
    print time.asctime(), "Server Stops - %s:%s" % (HOST_NAME, PORT_NUMBER)
