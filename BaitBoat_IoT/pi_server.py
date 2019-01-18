import newEngineControl
from socket import *
from time import ctime
import RPi.GPIO as GPIO

mc = newEngineControl.motorControl()

HOST = ''
PORT = 21567
BUFSIZE = 1024
ADDR = (HOST,PORT)

tcpSerSock = socket(AF_INET, SOCK_STREAM)
tcpSerSock.setsockopt(SOL_SOCKET, SO_REUSEADDR, 1)
tcpSerSock.bind(ADDR)
tcpSerSock.listen(5)


# receive method that makes sure we have the entire message
def recv_msg(the_socket, bufsize):
    total_data = []
    while True:
        data = the_socket.recv(bufsize).decode()
        if not data: break
        total_data.append(data)
    return ''.join(total_data)

while True:
        print ('Waiting for connection')
        tcpCliSock,addr = tcpSerSock.accept()
        print ('...connected from :', addr)
        try:
                while True:
                        data = ''
                        data = recv_msg(tcpCliSock, BUFSIZE)
                
                        if not data:
                                break
                        
                        if data == 'Left':
                                mc.steerLeft()
    
                        if data == 'Right':
                                mc.steerRight()
                                
                        if data == 'Ahead':
                                mc.straightAhead()
                                
                        if data == 'Reverse':
                                mc.reverse()
                            
                        if data == 'Stop':
                                mc.stopMotor()
                                
                        if data == 'StartCogMotor':
                                mc.cogMotorStart()
                            
                        if data == 'StopCogMotor':
                                mc.cogMotorStop()
                            
                            
        except KeyboardInterrupt:
                Servomotor.close()
                GPIO.cleanup()
tcpSerSock.close();