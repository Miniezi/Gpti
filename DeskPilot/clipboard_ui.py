"""Clipboard bindings that do not depend on the Windows keyboard layout."""
import os
import tkinter as tk
from tkinter import ttk


def editable(widget):
    return str(widget.cget('state')) not in ('disabled', 'readonly')


def password(widget):
    return not isinstance(widget, tk.Text) and bool(widget.cget('show'))


def select_all(widget):
    if isinstance(widget, tk.Text):
        widget.tag_add('sel', '1.0', 'end-1c')
        widget.mark_set('insert', 'end-1c')
    else:
        widget.selection_range(0, 'end')
        widget.icursor('end')
    return 'break'


def paste(widget):
    if not editable(widget):
        return 'break'
    try:
        value = widget.clipboard_get()
    except tk.TclError:
        widget.bell()
        return 'break'
    if isinstance(widget, tk.Text):
        if widget.tag_ranges('sel'):
            widget.delete('sel.first', 'sel.last')
        widget.insert('insert', value)
        widget.see('insert')
    else:
        # Keys/URLs must remain single-line when copied with a trailing newline.
        value = value.replace('\r', '').replace('\n', '')
        if widget.selection_present():
            first = widget.index('sel.first')
            widget.delete('sel.first', 'sel.last')
            widget.icursor(first)
        widget.insert('insert', value)
    return 'break'


def copy_or_cut(widget, cut=False):
    if password(widget) or (cut and not editable(widget)):
        return 'break'
    try:
        if isinstance(widget, tk.Text):
            value = widget.get('sel.first', 'sel.last')
        else:
            if not widget.selection_present():
                return 'break'
            value = widget.get()[widget.index('sel.first'):widget.index('sel.last')]
        widget.clipboard_clear()
        widget.clipboard_append(value)
        if cut:
            widget.delete('sel.first', 'sel.last')
    except tk.TclError:
        pass
    return 'break'


def shortcut(event):
    # VK_A/C/V/X stay the same for both EN and RU layouts on Windows.
    if event.state & (0x8 | 0x20000):  # Do not intercept AltGr combinations.
        return None
    key = ({65:'a', 67:'c', 86:'v', 88:'x'}.get(event.keycode)
           if os.name == 'nt' else event.keysym.lower())
    if key == 'v': return paste(event.widget)
    if key == 'a': return select_all(event.widget)
    if key == 'c': return copy_or_cut(event.widget)
    if key == 'x': return copy_or_cut(event.widget, cut=True)
    return None


def attach(widget):
    if getattr(widget, '_clipboard_menu', None) is not None:
        return
    menu = tk.Menu(widget, tearoff=False)
    menu.add_command(label='Вырезать', accelerator='Ctrl+X', command=lambda:copy_or_cut(widget, True))
    menu.add_command(label='Копировать', accelerator='Ctrl+C', command=lambda:copy_or_cut(widget))
    menu.add_command(label='Вставить', accelerator='Ctrl+V', command=lambda:paste(widget))
    menu.add_separator()
    menu.add_command(label='Выделить всё', accelerator='Ctrl+A', command=lambda:select_all(widget))
    widget._clipboard_menu = menu

    def popup(event):
        widget.focus_set()
        secret = password(widget)
        menu.entryconfigure(0, state='normal' if editable(widget) and not secret else 'disabled')
        menu.entryconfigure(1, state='disabled' if secret else 'normal')
        menu.entryconfigure(2, state='normal' if editable(widget) else 'disabled')
        try:
            menu.tk_popup(event.x_root, event.y_root)
        finally:
            menu.grab_release()
        return 'break'

    widget.bind('<Control-KeyPress>', shortcut)
    widget.bind('<Shift-Insert>', lambda e:paste(widget))
    widget.bind('<<Paste>>', lambda e:paste(widget))
    widget.bind('<<Copy>>', lambda e:copy_or_cut(widget))
    widget.bind('<<Cut>>', lambda e:copy_or_cut(widget, True))
    widget.bind('<Button-3>', popup)
    widget.bind('<Shift-F10>', lambda e:popup(type('Point', (), {
        'x_root':widget.winfo_rootx()+12, 'y_root':widget.winfo_rooty()+12})()))


def attach_tree(parent):
    for child in parent.winfo_children():
        if isinstance(child, (tk.Text, tk.Entry, ttk.Entry, ttk.Combobox)):
            attach(child)
        attach_tree(child)
