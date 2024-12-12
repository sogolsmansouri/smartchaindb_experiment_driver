import socket
import cherrypy
import subprocess
from subprocess import Popen, PIPE


class HelloWorld(object):
    @cherrypy.expose
    def GJoin(self, id, usk):
        # http://localhost:8080/GJoin?id=12&usk=1
        # cargo test test_scenario_1 --release --no-default-features --features PS_Signature_G1 -- GJoin,1,spk --nocapture
        dangerousString = 'cargo test test_scenario_1 --release --no-default-features --features PS_Signature_G1 -- ' + \
            'GJoin,'+id+','+usk + ' --nocapture'
        p = Popen(dangerousString, stderr=PIPE, stdout=PIPE, shell=True)
        output, err = p.communicate(
            b"input data that is passed to subprocess' stdin")
        key1 = str(output).split('Your secret: ')[1].split('\\n')[0]
        key2 = str(output).split('Group Public: ')[1].split('\\n')[0]
        return key1+'|'+key2

    def GSign(self, sk, m):
        return "return signature and message"

    def GVerify(self, pk, sk, m, sigma):
        return "return 0 or 1"

    @cherrypy.expose
    def PKIJoin(self, label):
        # http://localhost:8080/PKIJoin?label=sds
        #  cargo test test_scenario_1 --release --no-default-features --features PS_Signature_G1 -- PKIJoin,sdsd --nocapture
        dangerousString = 'cargo test test_scenario_1 --release --no-default-features --features PS_Signature_G1 -- ' + \
            'PKIJoin,'+label + ' --nocapture'
        p = Popen(dangerousString, stderr=PIPE, stdout=PIPE, shell=True)
        output, err = p.communicate(
            b"input data that is passed to subprocess' stdin")
        key = str(output).split('Your secret:')[1].split('\\n')[0]
        return key

    @cherrypy.expose
    def index(self):
        # Sample page that displays the number of records in "table"
        # Open a cursor, using the DB connection for the current thread
        return """
        -- Group Manager Server --
        GJoin(id, usk)
        """
    # response = await fetch("//localhost:8080/GJoin?id=12&usk=1");
    # alert(await response.text);


cherrypy.server.socket_host = socket.gethostname()
cherrypy.quickstart(HelloWorld())
