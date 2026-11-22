# syntax=docker/dockerfile:1

# Seed delle illustrazioni delle carte (servizio seed-images di compose.yaml): Python + Pillow per la conversione
# in WebP. Le immagini non sono nell'immagine: compose monta "docs/Carte definitive" in /carte in sola lettura.
FROM python:3.13-alpine
RUN pip install --no-cache-dir pillow==11.3.0 \
    && addgroup -S seed && adduser -S seed -G seed
WORKDIR /app
COPY seed.py .
USER seed
CMD ["python", "-u", "seed.py"]
