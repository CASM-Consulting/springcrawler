from flask import Flask
from flask import request
from flask import Response
import datetime
import dateparser
import json


app = Flask(__name__)
@app.route('/parse', )
def hello_world():
    relative_expression = request.args.get('relative_expression')
    #languages = json.loads(request.args.get('languages'))

    print(relative_expression)
    #print(languages)

    parsed = dateparser.parse(relative_expression)

    response = {
        'parsed' : parsed.strftime("%Y-%m-%d %H:%M:%S")
    }
    
    return Response(json.dumps(response), mimetype='application/json')

if __name__ == '__main__':
    app.run(port=5555)
