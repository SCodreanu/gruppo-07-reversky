"""Misura le due sfere in alto (ATK a sinistra, DEF a destra) nelle illustrazioni delle creature.

Le cornici non hanno le sfere tutte nello stesso punto né della stessa misura (cambiano con rarità e teoria),
quindi il web scrive ATK e DEF usando la misura di ogni carta. Questo script la ricava dalle immagini di
"docs/Carte definitive" (vedi mappa-carte.csv) e scrive reversky-web/src/components/cards/card-orbs.json:
    { "<id carta>": [[x, y, r], [x, y, r]], ... }   in % della larghezza (x, r) e dell'altezza (y) della carta.
Va rilanciato quando cambiano le illustrazioni delle creature:
    python scripts/seed-immagini/misura-sfere.py
Serve Python con numpy e Pillow.
"""

import csv
import json
import os
import sys
from collections import deque

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
FOLDER = os.path.join(ROOT, "docs", "Carte definitive")
OUTPUT = os.path.join(ROOT, "reversky-web", "src", "components", "cards", "card-orbs.json")
COARSE_WIDTH = 530
FINE_WIDTH = 1060


def integral(channel):
    return np.pad(channel.cumsum(0).cumsum(1), ((1, 0), (1, 0)))


def box_sum(table, cy, cx, half):
    return (table[cy + half + 1, cx + half + 1] - table[cy - half, cx + half + 1]
            - table[cy + half + 1, cx - half] + table[cy - half, cx - half])


def coarse_left_orb(pixels):
    """Centro e raggio della sfera di sinistra: zona di colore uniforme che contrasta con l'anello intorno."""
    height, width, _ = pixels.shape
    tables = [(integral(pixels[:, :, c]), integral(pixels[:, :, c] ** 2)) for c in range(3)]
    best = None
    for radius in range(int(width * 0.035), int(width * 0.075)):
        inner, middle, outer = max(2, int(radius * 0.5)), int(radius * 1.05), int(radius * 1.35)
        ys = np.arange(max(outer + 1, int(height * 0.02)), int(height * 0.12))
        xs = np.arange(max(outer + 1, int(width * 0.04)), int(width * 0.20))
        cy, cx = np.meshgrid(ys, xs, indexing="ij")
        n_inner, n_middle, n_outer = (2 * inner + 1) ** 2, (2 * middle + 1) ** 2, (2 * outer + 1) ** 2
        contrast = variance = 0
        for table, squares in tables:
            mean_inner = box_sum(table, cy, cx, inner) / n_inner
            variance = variance + box_sum(squares, cy, cx, inner) / n_inner - mean_inner ** 2
            mean_ring = (box_sum(table, cy, cx, outer) - box_sum(table, cy, cx, middle)) / (n_outer - n_middle)
            contrast = contrast + (mean_inner - mean_ring) ** 2
        score = np.sqrt(contrast) - np.sqrt(np.maximum(variance, 0))
        k = np.unravel_index(np.argmax(score), score.shape)
        if best is None or score[k] > best[0]:
            best = (score[k], int(cx[k]), int(cy[k]), radius)
    return best[1], best[2], best[3]


def refine(pixels, cx, cy, radius):
    """Bordo esatto della sfera: area collegata dello stesso colore del centro. Se il risultato non è plausibile
    resta la stima di partenza."""
    height, width, _ = pixels.shape
    y0, y1 = int(max(0, cy - 1.8 * radius)), int(min(height, cy + 1.8 * radius))
    x0, x1 = int(max(0, cx - 1.8 * radius)), int(min(width, cx + 1.8 * radius))
    patch = pixels[y0:y1, x0:x1]
    yy, xx = np.mgrid[y0:y1, x0:x1]
    inner = (xx - cx) ** 2 + (yy - cy) ** 2 < (0.45 * radius) ** 2
    if not inner.any():
        return cx, cy, radius
    reference = np.median(patch[inner], axis=0)
    distance = np.sqrt(((patch - reference) ** 2).sum(2))
    spread = np.sqrt(((patch[inner] - reference) ** 2).sum(1))
    mask = distance < max(28, np.percentile(spread, 90) * 1.6)
    h, w = mask.shape
    start = (int(cy - y0), int(cx - x0))
    if not (0 <= start[0] < h and 0 <= start[1] < w) or not mask[start]:
        return cx, cy, radius
    seen = np.zeros_like(mask)
    seen[start] = True
    queue, points = deque([start]), []
    while queue:
        y, x = queue.popleft()
        points.append((y, x))
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < h and 0 <= nx < w and mask[ny, nx] and not seen[ny, nx]:
                seen[ny, nx] = True
                queue.append((ny, nx))
    points = np.array(points)
    ys, xs = points[:, 0] + y0, points[:, 1] + x0
    # Estremi e non baricentro: i riflessi della sfera spostano il baricentro
    new_x = (np.percentile(xs, 1) + np.percentile(xs, 99)) / 2
    new_y = (np.percentile(ys, 1) + np.percentile(ys, 99)) / 2
    new_r = ((np.percentile(xs, 99) - np.percentile(xs, 1)) + (np.percentile(ys, 99) - np.percentile(ys, 1))) / 4
    if not (0.6 * radius <= new_r <= 1.5 * radius) or abs(new_x - cx) > 0.6 * radius or abs(new_y - cy) > 0.6 * radius:
        return cx, cy, radius
    return new_x, new_y, new_r


def measure(path):
    image = Image.open(path).convert("RGB")
    fine = np.asarray(image.resize((FINE_WIDTH, round(image.height * FINE_WIDTH / image.width))), dtype=np.float32)
    coarse = np.asarray(image.resize((COARSE_WIDTH, round(image.height * COARSE_WIDTH / image.width))), dtype=np.float32)
    height = fine.shape[0]
    scale = FINE_WIDTH / COARSE_WIDTH
    cx, cy, radius = coarse_left_orb(coarse)
    lx, ly, lr = refine(fine, cx * scale, cy * scale, radius * scale)
    # La cornice è simmetrica: la sfera di destra si cerca partendo dallo specchio di quella di sinistra
    rx, ry, rr = refine(fine, FINE_WIDTH - lx, ly, lr)
    # Se le due misure non tornano, una ha preso solo il centro piatto della sfera: vale la più grande, specchiata
    if abs(lr - rr) > 0.15 * max(lr, rr):
        if lr > rr:
            rx, ry, rr = FINE_WIDTH - lx, ly, lr
        else:
            lx, ly, lr = FINE_WIDTH - rx, ry, rr
    as_percent = lambda x, y, r: [round(x / FINE_WIDTH * 100, 2), round(y / height * 100, 2), round(r / FINE_WIDTH * 100, 2)]
    return [as_percent(lx, ly, lr), as_percent(rx, ry, rr)]


def main():
    with open(os.path.join(FOLDER, "mappa-carte.csv"), encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(line for line in handle if not line.lstrip().startswith("#")))
    # Solo le creature hanno ATK e DEF: sono nelle cartelle delle teorie
    creatures = [row for row in rows if row["file"].startswith("Teoria")]
    result = {}
    for row in creatures:
        result[row["card_id"]] = measure(os.path.join(FOLDER, row["file"]))
        print(f"{row['card_id']:>4} {row['card_name']}: {result[row['card_id']]}", flush=True)
    ordered = dict(sorted(result.items(), key=lambda item: int(item[0])))
    with open(OUTPUT, "w", encoding="utf-8") as handle:
        json.dump(ordered, handle, separators=(",", ":"))
        handle.write("\n")
    print(f"{len(ordered)} creature misurate in {OUTPUT}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
