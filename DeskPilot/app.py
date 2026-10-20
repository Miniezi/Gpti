import json
import os
from pathlib import Path
import queue
import threading
import tkinter as tk
from tkinter import ttk, messagebox, scrolledtext
from core import API, Agent, LocalTools

CONFIG=Path(os.environ.get('APPDATA',str(Path.home())))/'DeskPilot'/'settings.json'

class App(tk.Tk):
    def __init__(self):
        super().__init__(); self.title('DeskPilot • AI для Windows'); self.geometry('1050x800')
        self.events=queue.Queue(); self.stop=threading.Event(); self.busy=False; self.agent=None; self.dialog=None
        self.url=tk.StringVar(value='https://api.openai.com/v1'); self.key=tk.StringVar(); self.model=tk.StringVar()
        self.output=tk.StringVar(value='2048'); self.window=tk.StringVar(value='24000'); self.status=tk.StringVar(value='Готово')
        self.saved_key_scope=None
        try:
            data=json.loads(CONFIG.read_text('utf-8')); self.url.set(data['url']); self.model.set(data.get('model',''))
        except (OSError,ValueError,KeyError): pass
        top=ttk.Frame(self,padding=12); top.pack(fill='x')
        ttk.Label(top,text='API URL (включая /v1)').grid(row=0,column=0,sticky='w')
        ttk.Entry(top,textvariable=self.url,width=65).grid(row=0,column=1,sticky='ew')
        ttk.Label(top,text='API-ключ').grid(row=1,column=0,sticky='w')
        ttk.Entry(top,textvariable=self.key,show='•',width=65).grid(row=1,column=1,sticky='ew')
        ttk.Button(top,text='Сохранить ключ',command=self.save_key).grid(row=1,column=2,padx=6)
        ttk.Button(top,text='Загрузить ключ',command=self.load_key).grid(row=0,column=2,padx=6)
        ttk.Label(top,text='Модель').grid(row=2,column=0,sticky='w')
        self.models=ttk.Combobox(top,textvariable=self.model,width=62); self.models.grid(row=2,column=1,sticky='ew')
        ttk.Button(top,text='Обновить модели',command=self.refresh).grid(row=2,column=2,padx=6)
        top.columnconfigure(1,weight=1)
        opts=ttk.Frame(self,padding=(12,0)); opts.pack(fill='x')
        ttk.Label(opts,text='Макс. токенов ответа:').pack(side='left'); ttk.Entry(opts,textvariable=self.output,width=7).pack(side='left')
        ttk.Label(opts,text='   Контекст, символов ≈:').pack(side='left'); ttk.Entry(opts,textvariable=self.window,width=9).pack(side='left')
        ttk.Button(opts,text='Новая сессия',command=self.reset).pack(side='right')
        ttk.Label(self,text='Каждое действие — после подтверждения. Стоп: кнопка или Ctrl+Shift+F12; мышь — в угол экрана.',padding=12).pack(fill='x')
        self.chat=scrolledtext.ScrolledText(self,wrap='word',state='disabled',font=('Segoe UI',11)); self.chat.pack(fill='both',expand=True,padx=12)
        self.prompt=scrolledtext.ScrolledText(self,height=5,wrap='word',font=('Segoe UI',11)); self.prompt.pack(fill='x',padx=12,pady=8)
        buttons=ttk.Frame(self,padding=(12,0,12,12)); buttons.pack(fill='x')
        self.send=ttk.Button(buttons,text='Отправить',command=self.start); self.send.pack(side='left')
        ttk.Button(buttons,text='СТОП',command=self.halt).pack(side='left',padx=8)
        ttk.Label(buttons,textvariable=self.status).pack(side='left',padx=12)
        self.after(80,self.poll); self.protocol('WM_DELETE_WINDOW',self.close)
        self.bind('<Control-Shift-F12>',lambda e:self.halt())
        try:
            import keyboard
            keyboard.add_hotkey('ctrl+shift+f12',lambda:self.events.put(('stop',None)))
        except Exception: self.log('Глобальная горячая клавиша недоступна; используйте кнопку Стоп.')
        self.log('Укажите API URL и ключ, загрузите модели. Модель можно вписать вручную.\nСнимки и разрешённое содержимое файлов отправляются выбранному API. Команды выполняются с правами вашей учётной записи.')
    def log(self,s):
        self.chat.configure(state='normal'); self.chat.insert('end',s+'\n\n'); self.chat.see('end'); self.chat.configure(state='disabled')
    def load_key(self):
        try:
            import keyring
            scope=self.url.get().rstrip('/'); key=keyring.get_password('DeskPilot',scope)
            if key: self.key.set(key); self.saved_key_scope=scope
            else: messagebox.showinfo('Ключ','Для этого URL ключ не сохранён.')
        except Exception as e: messagebox.showerror('Хранилище ключей',str(e))
    def save_key(self):
        try:
            import keyring
            API(self.url.get().strip(),self.key.get())
            keyring.set_password('DeskPilot',self.url.get().strip().rstrip('/'),self.key.get())
            messagebox.showinfo('Ключ','Ключ сохранён в системном хранилище.')
        except Exception as e: messagebox.showerror('Хранилище ключей',str(e))
    def refresh(self):
        try: api=API(self.url.get().strip(),self.key.get())
        except Exception as e: messagebox.showerror('API',str(e)); return
        self.status.set('Загрузка моделей…')
        def work():
            try: self.events.put(('models',api.models()))
            except Exception as e: self.events.put(('error',str(e)))
        threading.Thread(target=work,daemon=True).start()
    def approve(self,name,args):
        event=threading.Event(); box={'ok':False}
        self.events.put(('approval',(name,args,event,box)))
        while not event.wait(.1):
            if self.stop.is_set(): return False
        return box['ok'] and not self.stop.is_set()
    def show_approval(self,item):
        name,args,event,box=item
        if self.stop.is_set(): event.set(); return
        win=tk.Toplevel(self); self.dialog=win; win.title('Разрешить действие ИИ?'); win.geometry('850x580'); win.transient(self)
        ttk.Label(win,text='Инструмент: '+name,font=('Segoe UI',14),padding=12).pack(anchor='w')
        ttk.Label(win,text='Проверьте все аргументы. run может выполнять любые команды и менять весь ПК.\nGUI-действие начнётся через 2 секунды после разрешения: выберите нужное окно.',padding=12).pack(anchor='w')
        text=scrolledtext.ScrolledText(win,wrap='word'); text.pack(fill='both',expand=True,padx=12)
        text.insert('1.0',json.dumps(args,ensure_ascii=False,indent=2)); text.configure(state='disabled')
        def answer(ok):
            box['ok']=ok; event.set(); self.dialog=None; win.destroy()
        b=ttk.Frame(win,padding=12); b.pack(fill='x')
        ttk.Button(b,text='Отказать',command=lambda:answer(False)).pack(side='right')
        ttk.Button(b,text='Разрешить ОДИН раз',command=lambda:answer(True)).pack(side='right',padx=12)
        win.protocol('WM_DELETE_WINDOW',lambda:answer(False)); win.grab_set()
    def start(self):
        if self.busy: return
        prompt=self.prompt.get('1.0','end').strip()
        if not prompt: return
        try:
            api=API(self.url.get().strip(),self.key.get()); model=self.model.get().strip()
            if not model: raise ValueError('Выберите модель')
            output=int(self.output.get()); window=int(self.window.get())
            if not 128<=output<=65536 or not 4000<=window<=500000: raise ValueError('Ответ: 128–65536 токенов; контекст: 4000–500000 символов')
        except Exception as e: messagebox.showerror('Настройки',str(e)); return
        fingerprint=(api.url,api.key,model,output,window)
        if self.agent is None or self.fingerprint!=fingerprint:
            self.agent=Agent(api,model,LocalTools(self.approve,self.stop),lambda s:self.events.put(('log',s)),self.stop,output,window)
            self.fingerprint=fingerprint; self.log('Начата новая сессия с выбранным API и моделью.')
        CONFIG.parent.mkdir(parents=True,exist_ok=True)
        CONFIG.write_text(json.dumps({'url':api.url,'model':model}),encoding='utf-8')
        self.prompt.delete('1.0','end'); self.log('Вы: '+prompt); self.busy=True; self.stop.clear(); self.send.configure(state='disabled'); self.status.set('Работаю…')
        def work():
            try: self.agent.run(prompt)
            except Exception as e: self.events.put(('error',str(e)))
            finally: self.events.put(('done',None))
        threading.Thread(target=work,daemon=True).start()
    def halt(self):
        self.stop.set(); self.status.set('Остановка… запрос API может завершаться до 120 секунд')
        if self.dialog:
            self.dialog.destroy(); self.dialog=None
    def reset(self):
        if self.busy: return
        self.agent=None; self.log('Контекст сессии очищен.')
    def close(self):
        self.halt()
        self.status.set('Закрываю после остановки текущей операции…')
        def finish():
            if self.busy: self.after(100,finish)
            else: self.destroy()
        finish()
    def poll(self):
        try:
            while True:
                kind,value=self.events.get_nowait()
                if kind=='log': self.log(value)
                elif kind=='error': self.log('Ошибка: '+value); self.status.set('Ошибка')
                elif kind=='models':
                    self.models['values']=value
                    if value and self.model.get() not in value: self.model.set(value[0])
                    self.status.set('Моделей: '+str(len(value)))
                elif kind=='approval': self.show_approval(value)
                elif kind=='stop': self.halt()
                elif kind=='done': self.busy=False; self.send.configure(state='normal'); self.status.set('Остановлено' if self.stop.is_set() else 'Готово')
        except queue.Empty: pass
        self.after(80,self.poll)

if __name__=='__main__':
    import sys
    app=App()
    if '--smoke-test' in sys.argv:
        app.after(1500,app.close)
    app.mainloop()
