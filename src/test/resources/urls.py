from utils import *

#__all__ = ['urls']

#you must supply this variable - this is what gets picked up by the Java code
urls = []

@get(r'test', urls)
def testFromJython(request, response):
    response.getOutputStream().write('some text from Jython!')

@get(r'test2', urls)
def test2(request, response):
    print 'in test2'
    response.getOutputStream().write('test2')

