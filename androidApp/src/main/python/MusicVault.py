import yt_dlp
import os
import urllib.request

def download_audio(url, output_dir):
    ydl_opts = {
        'format': 'bestaudio/best',
        'outtmpl': os.path.join(output_dir, '%(title)s.%(ext)s'),
        'quiet': True,
        'no_warnings': True,
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=True)

        # prepare_filename può restituire un'estensione diversa
        # dal file realmente scaricato, usiamo il titolo per trovarlo
        title = info.get('title', '')

        # Cerca il file reale nella cartella
        actual_file = None
        for f in os.listdir(output_dir):
            if f.startswith(title[:30]):  # confronto parziale per sicurezza
                actual_file = f
                break

        # Fallback a prepare_filename se non trovato
        if actual_file is None:
            path = ydl.prepare_filename(info)
            actual_file = os.path.basename(path)

        # Scarica thumbnail
        thumbnail_url = info.get('thumbnail', '')
        thumbnail_filename = ''
        if thumbnail_url:
            thumbnail_filename = os.path.splitext(actual_file)[0] + '.jpg'
            thumbnail_path = os.path.join(output_dir, thumbnail_filename)
            urllib.request.urlretrieve(thumbnail_url, thumbnail_path)

        return {
            'path': os.path.join(output_dir, actual_file),
            'filename': actual_file,
            'thumbnail_filename': thumbnail_filename,
            'title': title,
            'duration': info.get('duration', 0),
            'artist': info.get('uploader', ''),
        }

def get_info(url):
    """
    Recupera solo i metadati senza scaricare.
    Utile per mostrare anteprima prima del download.
    """
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
    }
    with yt_dlp.YoutubeDL(ydl_opts) as ydl:
        info = ydl.extract_info(url, download=True)
        path = ydl.prepare_filename(info)
        return {
            'path': path,
            'filename': os.path.basename(path),
            'title': info.get('title', ''),
            'duration': info.get('duration', 0),
            'thumbnail': info.get('thumbnail', ''),
            'artist': info.get('uploader', ''),
        }