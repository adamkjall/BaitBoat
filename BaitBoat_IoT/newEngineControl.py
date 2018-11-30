#!/usr/bin/python

import RPi.GPIO as GPIO   # Import the GPIO library.
import threading
import time

exitFlag = 0
steerLeftFlag = 0
steerRightFlag = 0
straightAheadFlag = 0
reverseFlag = 0
class motorControl ():

	# Pin constants using BCM standard.

	leftMotorPWM = 16
	rightMotorPWM = 18
	cogMotorPWM = 22
	# Speed constants
	fullSpeed = 1    # PWM (Motor Speed Control) 100 = +max rpm
	reversefullSpeed = 100    # PWM (Motor Speed Control) 1 = -max rpm
	cogSpeed = 100       # PWM (Cog Speed Control) 1 = -max rpm
	
	cogStarted = False

#	def __init__(self, threadID, name, counter):
	def __init__(self):
		print "Initialising ..."
		GPIO.setmode(GPIO.BOARD )  						# Use BCM standard for pin references.

		GPIO.setup(12,GPIO.OUT) # Set GPIO Pin 12 as output for left motor enable		
		GPIO.setup(10,GPIO.OUT) # Set GPIO Pin 10 as output for right motor enable
		GPIO.setup(8,GPIO.OUT)  # Set GPIO Pin 8 as output for cog motor enable		

		GPIO.setup(self.leftMotorPWM , GPIO.OUT)		# Set GPIO pin leftMotorPWM  to output mode.
		self.pwmLeft = GPIO.PWM(self.leftMotorPWM, 100)	# Initialize PWM on pwmPin 100Hz frequency
		self.pwmLeft.start( 0 )

		GPIO.setup(self.rightMotorPWM, GPIO.OUT)		# Set GPIO pin rightMotorPWM to output mode.
		self.pwmRight = GPIO.PWM(self.rightMotorPWM, 100) # Initialize PWM on pwmPin 100Hz frequency
		self.pwmRight.start( 0 )
		print("Initialised ,left PWM pin:%d, right PWM pin:%d, cog PWM pin:%d")  % (self.leftMotorPWM , self.rightMotorPWM, self.cogMotorPWM )

		GPIO.setup(self.cogMotorPWM, GPIO.OUT)			# Set GPIO pin cogMotorPWM  to output mode.
		self.pwmCog = GPIO.PWM(self.cogMotorPWM, 100)	# Initialize PWM on pwmPin 100Hz frequency
		self.pwmCog.start( 0 )

	def steerLeft(self):
		print "Going Left"
		GPIO.output(12,GPIO.HIGH)
		GPIO.output(10,GPIO.LOW)
		self.pwmRight.ChangeDutyCycle( self.fullSpeed  )
		self.pwmLeft.ChangeDutyCycle( 0 )

	def steerRight(self):
		print "Going Right"
		GPIO.output(12,GPIO.LOW)
		GPIO.output(10,GPIO.HIGH)
		self.pwmRight.ChangeDutyCycle( 0 )
		self.pwmLeft.ChangeDutyCycle( self.fullSpeed )
	
	def	straightAhead(self):
		print "Going Ahead"
		GPIO.output(12,GPIO.HIGH)
		GPIO.output(10,GPIO.HIGH)
		self.pwmRight.ChangeDutyCycle( self.fullSpeed )
		self.pwmLeft.ChangeDutyCycle( self.fullSpeed )
		
	def	reverse(self):
		print "Reversing"
		GPIO.output(12,GPIO.HIGH)
		GPIO.output(10,GPIO.HIGH)
		self.pwmRight.ChangeDutyCycle( self.reversefullSpeed )
		self.pwmLeft.ChangeDutyCycle( self.reversefullSpeed )		
		
	def	stopMotor(self):
		print "Stopping"
		GPIO.output(12,GPIO.LOW)
		GPIO.output(10,GPIO.LOW)
		self.pwmRight.ChangeDutyCycle( 0 )
		self.pwmLeft.ChangeDutyCycle( 0 )
		
	def	cogMotorStart(self):
		print "Cog Rotate"
		GPIO.output(8,GPIO.HIGH)
		self.pwmCog.ChangeDutyCycle( self.cogSpeed )

	def	cogMotorStop(self):
		print "Cog Stop"
		GPIO.output(8,GPIO.LOW)
		self.pwmCog.ChangeDutyCycle( 0 )

	def	run(self):
		print "Starting "
		while	True:
#			charIn = getch()
			charIn = raw_input()

			if charIn == 'a':
				self.steerLeft()
			elif charIn == 'd':
				self.steerRight()
			elif charIn == 'w':
				self.straightAhead()
			elif charIn == 'r':
				self.reverse()				
			elif charIn == 's':
				self.stopMotor()
			elif charIn == ' ':
				# If space is pressed and cog not running, start it.
				# If space is pressed and cog is running stop it.
				if self.cogStarted != True :
					self.cogMotorStart()
					self.cogStarted = True
				else:
					self.cogMotorStop()
					self.cogStarted = False
			elif charIn == 'x':
				self.stopMotor()
				self.cogMotorStop()
				GPIO.cleanup()
				print "Exiting "
				exit()



#===================================================
#
# Main loop
				
control = motorControl()

control.run()
