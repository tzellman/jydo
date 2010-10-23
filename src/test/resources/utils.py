__all__ = ['get', 'post', 'put', 'delete', 'head']

class verb:
    def __init__(self, urlRegex, urls):
        self.urlRegex = urlRegex
        self.urls = urls
    def __call__(self, *args):
        methName = self.__class__.__name__.upper()
        #the tuples in the urls variable are: (requestMethod, url, handlerFunc)
        self.urls.append((methName, self.urlRegex, args[0]))
class get(verb): pass
class post(verb): pass
class delete(verb): pass
class put(verb): pass
class head(verb): pass
