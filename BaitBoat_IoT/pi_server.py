import newEngineControl
from socket import *
from time import ctime
import RPi.GPIO as GPIO

mc = newEngineControl.motorControl()

HOST = ''
PORT = 21567
BUFSIZE = 16
ADDR = (HOST,PORT)

tcpSerSock = socket(AF_INET, SOCK_STREAM)
tcpSerSock.setsockopt(SOL_SOCKET, SO_REUSEADDR, 1)
tcpSerSock.bind(ADDR)
tcpSerSock.listen(5)


# receive method that makes sure we have the entire message
#def recv_msg(the_socket, bufsize):
#    total_data = []
#    while True:
#        data = the_socket.recv(bufsize).decode()
#        if not data: break
#        total_data.append(data)
#    return ''.join(total_data)

def process_message(message):
    if message == 'Left':
        mc.steerLeft()
    if message == 'Right':
        mc.steerRight()          
    if message == 'Ahead':
        mc.straightAhead()
        print ('Ahead')
    if message == 'Reverse':
        mc.reverse()           
    if message == 'Stop':
        mc.stopMotor()                 
    if message == 'StartCogMotor':
        mc.cogMotorStart()        
    if message == 'StopCogMotor':
        mc.cogMotorStop()
                       

while True:
        print ('Waiting for connection')
        tcpCliSock,addr = tcpSerSock.accept()
        print ('...connected from :', addr)
        try:
            buffer = ''
            i = 0
            while True:
                data = tcpCliSock.recv(BUFSIZE).decode()
                        #data = recv_msg(tcpCliSock, BUFSIZE)
                
                if not data:
                    if buffer:
                       process_message(buffer)
                    break
                        
                buffer += data
                commands = buffer.split('|')
                buffer = commands.pop()
                for command in commands:
                    print (i)
                    i += 1
                    process_message(command)
                        
                         
                            
        except KeyboardInterrupt:
                Servomotor.close()
                GPIO.cleanup()
tcpSerSock.close();