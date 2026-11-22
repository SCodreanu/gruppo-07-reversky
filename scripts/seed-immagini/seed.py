"""Seed delle illustrazioni delle carte (servizio seed-images di compose.yaml).

Legge /carte/mappa-carte.csv (file -> id della carta), riduce ogni immagine a WebP sotto i 2 MB dell'API
e la carica come admin con POST /admin/cards/{id}/image, come farà il backoffice.
Salta le carte il cui file d'illustrazione esiste già (FORCE=1 per ricaricarle tutte), quindi si può avviare
a ogni `docker compose up`. Funziona solo con il profilo dev, che crea l'utente admin dei dati di prova.
"""

import csv
import io
import json
import os
import sys
import time
import urllib.error
import urllib.request
import uuid

from PIL import Image

BASE = os.environ.get("API_URL", "http://api:8080").rstrip("/")
API = BASE + "/api/v1"
FOLDER = os.environ.get("CARDS_DIR", "/carte")
MAPPING = os.path.join(FOLDER, "mappa-carte.csv")
PROFILE = os.environ.get("SPRING_PROFILES_ACTIVE", "default")
EMAIL = os.environ.get("ADMIN_EMAIL", "admin@reversky.dev")
PASSWORD = os.environ.get("ADMIN_PASSWORD", "Reversky2026")
FORCE = os.environ.get("FORCE") == "1"
MAX_WIDTH = 750
MAX_BYTES = 2 * 1024 * 1024


def log(message):
    print(message, flush=True)


def call(method, path, token=None, body=None, content_type="application/json"):
    data = body if isinstance(body, (bytes, type(None))) else json.dumps(body).encode()
    request = urllib.request.Request(API + path, data=data, method=method)
    if data is not None:
        request.add_header("Content-Type", content_type)
    if token:
        request.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            text = response.read().decode("utf-8")
            return response.status, json.loads(text) if text else None
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode("utf-8", "replace")


def has_image(card):
    """True se il file dell'illustrazione esiste: i segnaposto delle migration rispondono 404.
    GET di un solo byte: l'API non ammette HEAD su /images/** (risponde 401)."""
    if not card.get("imageUrl"):
        return False
    request = urllib.request.Request(BASE + card["imageUrl"], headers={"Range": "bytes=0-0"})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status in (200, 206)
    except (urllib.error.URLError, OSError):
        return False


def wait_for_api(seconds=240):
    deadline = time.time() + seconds
    while time.time() < deadline:
        try:
            status, _ = call("GET", "/rules")
            if status == 200:
                return True
        except (urllib.error.URLError, OSError):
            pass
        time.sleep(3)
    return False


def to_webp(path):
    """WebP con la trasparenza degli angoli, largo al massimo MAX_WIDTH; qualità più bassa se supera i 2 MB."""
    with Image.open(path) as image:
        image = image.convert("RGBA")
        if image.width > MAX_WIDTH:
            image = image.resize((MAX_WIDTH, round(image.height * MAX_WIDTH / image.width)), Image.LANCZOS)
        for quality in (85, 75, 60, 45):
            buffer = io.BytesIO()
            image.save(buffer, "WEBP", quality=quality, method=4)
            if buffer.tell() <= MAX_BYTES:
                return buffer.getvalue()
    raise ValueError("ancora oltre 2 MB dopo la conversione")


def upload(card_id, content, token):
    boundary = uuid.uuid4().hex
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="image"; filename="card-{card_id}.webp"\r\n'
        "Content-Type: image/webp\r\n\r\n"
    ).encode() + content + f"\r\n--{boundary}--\r\n".encode()
    return call("POST", f"/admin/cards/{card_id}/image", token, body, f"multipart/form-data; boundary={boundary}")


def main():
    if PROFILE != "dev":
        log(f"Profilo '{PROFILE}': il seed delle illustrazioni gira solo con il profilo dev. Niente da fare.")
        return 0
    if not os.path.isfile(MAPPING):
        log(f"Manca {MAPPING}: niente da caricare.")
        return 0
    log("Attendo l'API...")
    if not wait_for_api():
        log("L'API non risponde: seed delle illustrazioni annullato.")
        return 1

    status, body = call("POST", "/auth/login", body={"email": EMAIL, "password": PASSWORD})
    if status != 200:
        log(f"Login di {EMAIL} non riuscito ({status}): seed delle illustrazioni annullato.")
        return 1
    token = body["accessToken"]

    cards, page = {}, 0
    while True:
        status, body = call("GET", f"/cards?page={page}&size=100", token)
        if status != 200:
            log(f"GET /cards non riuscito ({status})")
            return 1
        cards.update({card["id"]: card for card in body["content"]})
        page += 1
        if page >= body["totalPages"]:
            break

    with open(MAPPING, encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(line for line in handle if not line.lstrip().startswith("#")))

    uploaded = skipped = errors = 0
    for row in rows:
        file_name, card_name = row["file"], row["card_name"]
        card = cards.get(int(row["card_id"]))
        if card is None or card["name"] != card_name:
            found = card["name"] if card else "nessuna carta"
            log(f"  SALTATA  {file_name}: la carta {row['card_id']} è '{found}', la mappa dice '{card_name}'")
            errors += 1
            continue
        if not FORCE and has_image(card):
            skipped += 1
            continue
        path = os.path.join(FOLDER, file_name)
        try:
            content = to_webp(path)
        except (OSError, ValueError) as error:
            log(f"  ERRORE   {file_name}: {error}")
            errors += 1
            continue
        status, body = upload(card["id"], content, token)
        if status == 200:
            log(f"  OK       {file_name} -> {card['id']} '{card_name}' ({len(content) // 1024} KB)")
            uploaded += 1
        else:
            log(f"  ERRORE   {file_name} -> {card['id']}: {status} {body}")
            errors += 1

    unmapped = len(cards) - len({int(row["card_id"]) for row in rows})
    log(f"Caricate: {uploaded}. Già presenti: {skipped}. Errori: {errors}. Carte senza immagine nella mappa: {unmapped}.")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
