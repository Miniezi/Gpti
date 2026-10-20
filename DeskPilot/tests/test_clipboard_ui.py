import os
import unittest
from types import SimpleNamespace

@unittest.skipUnless(os.name == 'nt', 'Native Windows clipboard and Tk required')
class ClipboardTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        import tkinter as tk
        from tkinter import ttk
        from clipboard_ui import attach_tree
        cls.root=tk.Tk()
        cls.entry=ttk.Entry(cls.root); cls.entry.pack()
        cls.key=ttk.Entry(cls.root,show='*'); cls.key.pack()
        cls.combo=ttk.Combobox(cls.root); cls.combo.pack()
        cls.text=tk.Text(cls.root); cls.text.pack()
        attach_tree(cls.root); cls.root.update()
    @classmethod
    def tearDownClass(cls): cls.root.destroy()
    def setUp(self):
        self.root.clipboard_clear(); self.root.clipboard_append('тест-API-123')
        for w in (self.entry,self.key,self.combo): w.delete(0,'end')
        self.text.configure(state='normal'); self.text.delete('1.0','end')
        self.root.update()
    def test_ctrl_v_in_russian_layout(self):
        from clipboard_ui import shortcut
        for w in (self.entry,self.key,self.combo,self.text):
            shortcut(SimpleNamespace(widget=w,keycode=86,keysym='Cyrillic_em',state=4))
            result=w.get('1.0','end-1c') if w is self.text else w.get()
            self.assertEqual(result,'тест-API-123')
    def test_real_ctrl_v_binding(self):
        self.key.focus_force(); self.root.update()
        self.key.event_generate('<Control-KeyPress-v>'); self.root.update()
        self.assertEqual(self.key.get(),'тест-API-123')
    def test_shift_insert_and_replace_selection(self):
        self.entry.insert(0,'old'); self.entry.selection_range(0,'end')
        self.entry.focus_force(); self.root.update()
        self.entry.event_generate('<Shift-Insert>'); self.root.update()
        self.assertEqual(self.entry.get(),'тест-API-123')
    def test_context_menu_paste_password(self):
        self.key._clipboard_menu.invoke(2)
        self.assertEqual(self.key.get(),'тест-API-123')
        self.assertTrue(self.key.bind('<Button-3>'))
    def test_copy_readonly_text_and_block_paste(self):
        from clipboard_ui import select_all,copy_or_cut,paste
        self.text.insert('1.0','output'); select_all(self.text)
        self.text.configure(state='disabled'); copy_or_cut(self.text)
        self.assertEqual(self.root.clipboard_get(),'output')
        paste(self.text); self.assertEqual(self.text.get('1.0','end-1c'),'output')
    def test_empty_clipboard(self):
        from clipboard_ui import paste
        self.root.clipboard_clear(); paste(self.key)
        self.assertEqual(self.key.get(),'')

if __name__=='__main__': unittest.main()
