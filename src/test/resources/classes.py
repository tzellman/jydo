from utils import *
from java.io import ByteArrayInputStream as ByteStream
from java.lang import String


class Router(object):
    def foo(self):
        return 'foo'
    
    def doDynamic(self, request, response):
        response.getOutputStream().write('doDynamic')

def route():
    return Router()


def doIndex(request, response):
    response.getOutputStream().write('doIndex')
