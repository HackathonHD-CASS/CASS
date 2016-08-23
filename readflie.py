import time
import os
import math
import random
import sys

class Car:
    pre_x_cor = 0
    pre_y_cor = 0
    pre_spds = []

car = Car()

MIN_SPEED = 15
ACC_MUL = 2
MIN_ACC = 0

IP='10.10.100.184'

PRE = 3

DEG_ERR = 360

def cal_Suddenstop(pres, now, r_type):
	N = len(pres)

	if N==PRE:
		pre_acc = []
		if r_type == 'E':
			ACC_TH = 9
		elif r_type == 'N':
			ACC_TH = 7
		else:
			ACC_TH = 5
		for i in range(N-1):
			tmp = pres[i] - pres[i+1]
			if tmp < 0:
				return 'n'
			pre_acc.append(tmp)
		now_acc = pres[-1] - now
		if now_acc < 0:
			return 'n'

		if (sum(pres)/N) > MIN_SPEED:
			'''
			print now_acc, ACC_TH, now_acc>ACC_TH
			if now_acc>ACC_TH: print 'in func:',pres, now
			'''
			if now_acc > ACC_TH and now_acc > ACC_MUL*(sum(pre_acc)/len(pre_acc))+MIN_ACC:
				return 'y'
	return 'n'

def cal_Dir(pre_x, pre_y, x, y):
    dif_x = x-pre_x
    dif_y = y-pre_y
    if not dif_x == 0:
		return math.degrees(math.atan(dif_y/dif_x))
    return DEG_ERR

direc = DEG_ERR
emergency = 'n'

dir_name = './data2/'

car_code = sys.argv[1]
if car_code == 'r':
    files = os.listdir(dir_name)
    car_code = random.choice(files)
dir_name += car_code+'/'

car_key = sys.argv[2]
if car_key == 'r':
    files = os.listdir(dir_name)
    car_key = random.choice(files)
dir_name += car_key+'/'

file_name = sys.argv[3]
if file_name == 'r':
    files = os.listdir(dir_name)
    file_name = random.choice(files[:-1])[:-8]

whole_name = dir_name+file_name+'.log.csv'

f = open(whole_name, 'r')
lines = f.readlines()

for l in lines:
    if not l[0] == ',':
		d = l.strip().split(',')
		if len(car.pre_spds) < PRE:
			car.pre_spds.append(int(d[7]))
			if not d[4] == '':
				car.x_cor = float(d[4])
				car.y_cor = float(d[5])	    
			if len(car.pre_spds) == PRE-1:
				car.car_id = d[1]
				car.speed = int(d[7])
				car.pre_spds.insert(0,0)
				car.pre_spds.pop()
		else:
			if not d[4] == '':
				car.pre_x_cor = car.x_cor
				car.x_cor = float(d[4])
				car_pre_y_cor = car.y_cor
				car.y_cor = float(d[5])
			car.pre_spds.pop(0)
			car.pre_spds.append(car.speed)
			car.speed = int(d[7])
			car.break_on = int(d[11])
			car.wheel_rad = float(d[13])
			car.road_type = d[17]

			isACC = cal_Suddenstop(car.pre_spds, car.speed, car.road_type)

			tmp = cal_Dir(car.pre_x_cor, car.pre_y_cor, car.x_cor, car.y_cor)
			if not tmp == DEG_ERR:
				direc = tmp

			if car.break_on == 1 and isACC == 'y':
				emergency = 'y'
			else:
				emergency = 'n'

			s = 'http://'+IP+':8080/c.js?'
			s+='uuid='+car.car_id
			s+='&velocity='+str(car.speed)
			s+='&ss='+isACC
			s+='&direction='+str(direc)
			s+='&roadType='+car.road_type
			s+='&emer='+emergency

			if isACC == 'y': print car.pre_spds, car.speed
			#os.system('curl -I "'+s+'"')
		#time.sleep(0.1)