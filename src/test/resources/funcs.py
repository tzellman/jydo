from utils import *
from java.io import ByteArrayInputStream as ByteStream
from java.lang import String

def baz():
    return 'baz'

def bazStream():
    return ByteStream(String('bazStream').getBytes())

def doFooBar(request, response):
    response.getOutputStream().write('foobar')

def foo():
    def bar():
        return 'bar'
    return bar

def index():
    return 'index'

def doDynamic(context, request, response):
    response.getOutputStream().write(context)

