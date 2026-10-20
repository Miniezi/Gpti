import json
from pathlib import Path
import tempfile
import threading
import unittest
from core import API, Agent, LocalTools, context_size

class CoreTests(unittest.TestCase):
    def test_url_policy(self):
        for url in ['http://example.com/v1','https://key@host/v1','https://host/v1?key=secret']:
            with self.assertRaises(ValueError): API(url,'secret')
        API('http://localhost:1234/v1','')
    def test_denied_never_writes(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'no.txt'
            tools=LocalTools(lambda *x:False,threading.Event())
            self.assertIn('DENIED',tools.execute('write_file',{'path':str(p),'content':'bad'}))
            self.assertFalse(p.exists())
    def test_backup_and_read(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'yes.txt'; p.write_text('original')
            tools=LocalTools(lambda *x:True,threading.Event())
            result=json.loads(tools.execute('write_file',{'path':str(p),'content':'новый'}))
            self.assertEqual(Path(result['backup']).read_text(),'original')
            self.assertEqual(json.loads(tools.execute('read_file',{'path':str(p),'offset':1,'limit':2}))['text'],'ов')
    def test_stopped_never_approves(self):
        stop=threading.Event(); stop.set()
        tools=LocalTools(lambda *x:self.fail('approval called'),stop)
        self.assertEqual(tools.execute('run',{}),'STOPPED')
    def test_process_stop(self):
        import sys
        stop=threading.Event(); tools=LocalTools(lambda *x:True,stop)
        timer=threading.Timer(.2,stop.set); timer.start()
        try:
            result=json.loads(tools.execute('run',{'argv':[sys.executable,'-c','import time; time.sleep(20)'],'cwd':str(Path.cwd())}))
            self.assertIn('stopped',result['status'])
            self.assertNotEqual(result['exit_code'],0)
        finally: timer.cancel()
    def test_agent_tool_roundtrip(self):
        class Fake:
            def __init__(self): self.n=0
            def chat(self,model,messages,output):
                self.n+=1
                if self.n==1:
                    m={'role':'assistant','content':None,'tool_calls':[{'id':'a','type':'function','function':{'name':'list_dir','arguments':'{"path":"."}'}}]}
                else:
                    assert messages[-1]['role']=='tool'
                    m={'role':'assistant','content':'готово'}
                return {'choices':[{'message':m,'finish_reason':'stop'}],'usage':{'total_tokens':10}}
        stop=threading.Event(); api=Fake(); logs=[]
        agent=Agent(api,'mock',LocalTools(lambda *x:False,stop),logs.append,stop)
        agent.run('список файлов')
        self.assertEqual(api.n,2); self.assertEqual(agent.usage,20)
        self.assertIn('DENIED',agent.groups[0][2]['content'])
    def test_image_size_not_base64_chars(self):
        self.assertLess(context_size([{'content':'data:image/png;base64,'+'a'*500000}]),100)

if __name__=='__main__': unittest.main()
