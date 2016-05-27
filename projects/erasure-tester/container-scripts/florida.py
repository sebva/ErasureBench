import subprocess
import threading
from http.server import HTTPServer, BaseHTTPRequestHandler


def florida_string():
    ps_output = subprocess.check_output('docker-compose ps redis-master', shell=True).decode().splitlines()
    ps_output_q = subprocess.check_output('docker-compose ps -q redis-master', shell=True).decode().splitlines()

    nodes_names = [x.split(' ')[0] for x in ps_output if x.startswith('erasuretester')]
    nodes_tokens = [str(int(x[:10], 16)) for x in ps_output_q]

    florida = [x[0] + ":8101:rack1:dc:" + x[1] for x in zip(nodes_names, nodes_tokens)]
    return '|'.join(florida).encode()


class FloridaHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/REST/v1/admin/get_seeds':
            self.send_response(200)
            self.send_header("Content-type", "text/plain")
            self.end_headers()
            self.wfile.write(florida_string())
        else:
            self.send_response(404)
            self.send_header("Content-type", "text/plain")
            self.end_headers()
            self.wfile.write(b"Nothing here, content is at /REST/v1/admin/get_seeds\n")


class florida_server:
    def __init__(self):
        self.server = HTTPServer(('', 4321), FloridaHandler)
        self.server_thread = threading.Thread(target=self.server.serve_forever)
        self.server_thread.daemon = True

    def __enter__(self):
        self.server_thread.start()
        print("Florida server started")

    def __exit__(self, exc_type, exc_val, exc_tb):
        print("Stopping Florida server")
        self.server.shutdown()
        print("Florida server stopped")
