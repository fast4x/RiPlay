import yt_dlp
import os
import urllib.request

def download_audio(url, output_dir):
    ydl_opts = {
        'format': 'bestaudio/best',
        'outtmpl': os.path.join(output_dir, '%(title)s [%(id)s].%(ext)s'),
        'quiet': True,
        'no_warnings': True,
        'http_headers': {
            'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
            'Accept-Language': 'en-US,en;q=0.9',
        },
        'extractor_args': {
            'youtube': {
                'player_client': ['android', 'web']
            }
        },
    }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)

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

            # Scarica thumbnail in modo sicuro
            thumbnail_url = info.get('thumbnail', '')
            thumbnail_filename = ''
            if thumbnail_url:
                thumbnail_filename = os.path.splitext(actual_file)[0] + '.jpg'
                thumbnail_path = os.path.join(output_dir, thumbnail_filename)
                try:
                    # Aggiungiamo l'User-Agent anche per l'immagine, altrimenti YouTube può dare 403
                    req = urllib.request.Request(thumbnail_url, headers={
                        'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36'
                    })
                    with urllib.request.urlopen(req) as response, open(thumbnail_path, 'wb') as out_file:
                        out_file.write(response.read())
                except Exception as thumb_e:
                    # Se la thumbnail fallisce, non blocchiamo tutto il download!
                    print(f"Errore thumbnail: {thumb_e}")
                    thumbnail_filename = ''

            return {
                'path': os.path.join(output_dir, actual_file),
                'filename': actual_file,
                'thumbnail_filename': thumbnail_filename,
                'title': title,
                'duration': info.get('duration', 0),
                'artist': info.get('uploader', ''),
            }

    except Exception as e:
        # Cattura qualsiasi errore di yt-dlp e ritornalo a Kotlin invece di far crashare tutto
        return {
            'error': str(e)
        }

def get_info(url):
    """
    Recupera solo i metadati senza scaricare.
    """
    ydl_opts = {
        'quiet': True,
        'no_warnings': True,
        'http_headers': {
            'User-Agent': 'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
        },
    }
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            # Uso download=False per non scaricare l'audio quando si chiama get_info
            info = ydl.extract_info(url, download=False)
            path = ydl.prepare_filename(info)
            return {
                'path': path,
                'filename': os.path.basename(path),
                'title': info.get('title', ''),
                'duration': info.get('duration', 0),
                'thumbnail': info.get('thumbnail', ''),
                'artist': info.get('uploader', ''),
            }
    except Exception as e:
        return {
            'error': str(e)
        }