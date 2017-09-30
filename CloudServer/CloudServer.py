import sys
import socket
import mysql.connector
import SocketServer
import re
import subprocess
from decimal import Decimal
from datetime import datetime
import threading
import parser
import numpy as np
import urllib, json

#------------
# commands:
# ip:m:label 			-- set label and start measurement
# ip:u:power_rate 	    -- return power cost
# ip:r:ip				-- register device and return Ok/Retry
# ip:s:label 		    -- return current status: 0/1 : Label
# ip:c:realtime         -- return current,voltage,power
#------------

# socket_ip = "192.168.43.216"
port = 9999
pid = None
pid_dict = {}

mysql_pswd = '12345678'
lockOne=threading.Lock()
#-------------
# Check if IP is valid
def validIP(ip):
	try:
		socket.inet_pton(socket.AF_INET, ip)
	except socket.error:
		parser.error("Invalid IP Address.")
	return ip

# Predefined Smart Plug Commands
# For a full list of commands, consult tplink_commands.txt
commands = {'info'     : '{"system":{"get_sysinfo":{}}}',
			'on'       : '{"system":{"set_relay_state":{"state":1}}}',
			'off'      : '{"system":{"set_relay_state":{"state":0}}}',
			'cloudinfo': '{"cnCloud":{"get_info":{}}}',
			'wlanscan' : '{"netif":{"get_scaninfo":{"refresh":0}}}',
			'time'     : '{"time":{"get_time":{}}}',
			'schedule' : '{"schedule":{"get_rules":{}}}',
			'countdown': '{"count_down":{"get_rules":{}}}',
			'antitheft': '{"anti_theft":{"get_rules":{}}}',
			'reboot'   : '{"system":{"reboot":{"delay":1}}}',
			'power'	   : '{"emeter":{"get_realtime":{}}}',
			'reset'    : '{"system":{"reset":{"delay":1}}}',
			'emeter'   : '{"emeter":{"get_realtime":{}}}',
			'status'   : 'status',
			'realtime' : 'realtime'
}

#global label

label = ''


# Encryption and Decryption of TP-Link Smart Home Protocol
# XOR Autokey Cipher with starting key = 171
def encrypt(string):
	key = 171
	result = "\0\0\0\0"
	for i in string:
		a = key ^ ord(i)
		key = a
		result += chr(a)
	return result

def decrypt(string):
	key = 171
	result = ""
	for i in string:
		a = key ^ ord(i)
		key = ord(i)
		result += chr(a)
	return result




def check_status_name(socket_ip,label):
	status = "correct"
	#----------------------
	# Get realtime power
	cmd = '{"emeter":{"get_realtime":{}}}'
	try:
		sock_tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		sock_tcp.connect((socket_ip, port))
		sock_tcp.send(encrypt(cmd))
		data = sock_tcp.recv(2048)
		sock_tcp.close()
	except socket.error:
		quit("Cound not connect to host " + socket_ip + ":" + str(port))
	msg = decrypt(data[4:])
	realtime_power = Decimal(re.findall(r"power\":(.+?),",msg)[0])

	#------------------
	#Get name
	# sql = "SELECT name FROM plug ORDER BY id DESC LIMIT 1;"
	# cnx = mysql.connector.connect(user='root', password='12345678',
	# 							host='localhost',
	# 							database='tplink')
	# try:
	# 	cursor = cnx.cursor()
	# 	cursor.execute(sql)
	# 	for row in cursor.fetchall():
	# 		get_name = row[0]
	# except:
	# 	cnx.rollback()
	# cnx.close()
	get_name = label
	#------------------


	#------------------
	#Get power
	get_power = []
	sql = "SELECT power FROM plug WHERE name = \"" + get_name +"\";"
	cnx = mysql.connector.connect(user='root', password=mysql_pswd,
								host='localhost',
								database='tplink')
	try:
		cursor = cnx.cursor()
		cursor.execute(sql)
		for row in cursor.fetchall():
			get_power.append(row[0])
	except:
		cnx.rollback()
	cnx.close()

	# TODO
	# Use getpower, realtime_power to calculate the status.
	power_mean = np.mean(get_power)
	power_std = np.std(get_power)
	power_diff = np.max(get_power) - np.min(get_power)
	power_min = power_mean -  1*power_std
	power_max = power_mean +  1*power_std
	if realtime_power < power_min or realtime_power > power_max:
            status = "wrong"
	#result
	res = []
	realtime_power = str(status)
	res.append(realtime_power)
	res.append(get_name)
	return res


# def check_realtime():
#
# 	cmd = '{"emeter":{"get_realtime":{}}}'
# 	try:
# 		sock_tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# 		sock_tcp.connect((socket_ip, port))
# 		sock_tcp.send(encrypt(cmd))
# 		data = sock_tcp.recv(2048)
# 		sock_tcp.close()
# 	except socket.error:
# 		quit("Cound not connect to host " + socket_ip + ":" + str(port))
# 	msg = decrypt(data[4:])
# 	rt_current = Decimal(re.findall(r"current\":(.+?),",msg)[0])
# 	rt_voltage = Decimal(re.findall(r"voltage\":(.+?),",msg)[0])
# 	rt_power = Decimal(re.findall(r"power\":(.+?),",msg)[0])
# 	rt_res = []
# 	rt_res.append(rt_current)
# 	rt_res.append(rt_voltage)
# 	rt_res.append(rt_power)
# 	return rt_res

def month_cons(socket_ip):
	#----------------------
	# Get realtime cosumption
	cmd = '{"emeter":{"get_realtime":{}}}'
	try:
		sock_tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		sock_tcp.connect((socket_ip, port))
		sock_tcp.send(encrypt(cmd))
		data = sock_tcp.recv(2048)
		sock_tcp.close()
	except socket.error:
		quit("Cound not connect to host " + socket_ip + ":" + str(port))
	msg = decrypt(data[4:])
	use = Decimal(re.findall(r"total\":(.+?),",msg)[0])

	url_daily = "https://rrtp.comed.com/api?type=5minutefeed"
	jurl_daily = urllib.urlopen(url_daily)
	data_daily = json.load(jurl_daily)
	# print "data_daily length:", len(data_daily)
	price_daily = sum(float(data_daily[i]['price']) for i in range(len(data_daily)))/len(data_daily)
	fee = price_daily * float(use)
	return str(use), str(fee)

def check_register(plug_ip):
	res = 0
	cmd = '{"system":{"get_sysinfo":{}}}'
	try:
		sock_tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
		sock_tcp.connect((plug_ip, port))
		sock_tcp.send(encrypt(cmd))
		data = sock_tcp.recv(2048)
		sock_tcp.close()
		res = 1
		return res
	except socket.error:
		quit("Cound not connect to host " + plug_ip + ":" + str(port))

class MyTCPHandler(SocketServer.BaseRequestHandler):
    """
    The request handler class for our server.

    It is instantiated once per connection to the server, and must
    override the handle() method to implement communication to the
    client.
    """
    #-------------
    #command format
    #ip:command_type:command

    def handle(self):
        # self.request is the TCP socket connected to the client
        self.data = self.request.recv(2048)
        print self.data
        ip_cm = self.data
        cm_list = ip_cm.split(':')
        socket_ip = cm_list[0]
        cm_type = cm_list[1]
        cm = cm_list[2]
        print ip_cm

        if cm_type == "m":
            global pid
            label = cm
            print "change label:", label
            self.request.sendall("Set label to: "+ label)
            message = []
            message.append(socket_ip)
            message.append(label)
            # try:
			# 	pid.kill()
            # except:
            #     pass
            pid = pid_dict.get(socket_ip)
            if pid != None:
				pid.kill()
            lockOne.acquire()
            pid = subprocess.Popen(["python","test_subp.py",socket_ip,label])
            pid_dict[socket_ip]=pid
            lockOne.release()
        elif cm_type == "r":
			regis = check_register(socket_ip)
			print "rigsiter: ", regis
			if regis == 1:
				self.request.sendall("OK")
			else:
				self.request.sendall("Retry")
        elif cm_type == "u":
            #cost = cm
            # res_val = month_cons(socket_ip)
            # #total_cost = float(cost) * res_val
            # total_cost =  res_val
			power_usage, monthly_fee = month_cons(socket_ip)
			print "power usage: ", power_usage
			print "monthly fee: ", monthly_fee
			self.request.sendall("Monthly Power Usage: " + power_usage + " Kwh; Monthly Fee: "+monthly_fee)
        elif cm_type == "s":
			check_label = cm
			res_string = check_status_name(socket_ip,check_label)
			print "status", res_string[0]
			print "name", res_string[1]
			self.request.sendall(res_string[1] + ": "+ res_string[0])
        else:
			cmd = commands[cm]
			print "command: ", cmd
			sock_tcp = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
			sock_tcp.connect((socket_ip, port))
			sock_tcp.send(encrypt(cmd))
			data = sock_tcp.recv(2048)
			sock_tcp.close()
			msg = decrypt(data[4:])
			self.request.sendall(msg)
            # elif cmd == "realtime":
            #     res_string = check_realtime()
            #     print "current", res_string[0]
            #     print "voltage", res_string[1]
            #     print "power", res_string[2]
            #     self.request.sendall("current: "+ res_string[0] + " ,voltage: " + res_string[1] + ",power: "+ res_string[2])

if __name__ == "__main__":

    # HOST, PORT = "192.168.43.122", 8000
    # HOST, PORT = "192.168.43.29", 8000
	HOST, PORT = sys.argv[1], int(sys.argv[2])
    # Create the server, binding to localhost on port 9999
	server = SocketServer.TCPServer((HOST, PORT), MyTCPHandler)

    # Activate the server; this will keep running until you
    # interrupt the program with Ctrl-C
	server.serve_forever()
