"""Remake every Observer (Sublunary Eye pathway) asset: models, paintings, pictures, particles, icons and sounds.

python3 tools/model_gen/observer/generate.py src/main/resources/assets/circle_of_inevitability
python3 tools/model_gen/observer/generate.py <assets> --only models,icons     just those parts
python3 tools/model_gen/observer/generate.py <assets> --preview /tmp/obs       also an icon contact sheet there

Parts: models (box models, models.py), paintings (framed canvases, paintings.py), pictures (flat picture models,
pictures.py), particles (particles.py), icons (wheel, status and pathway icons, icons.py), sounds (sounds.py; it
also merges the observer.* events into sounds.json and their subtitles into lang/en_us.json). Each part only writes
its own files, so running one never touches the others. Sounds need numpy, scipy and ffmpeg; the rest need Pillow.
"""
import argparse
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

PARTS = ('models', 'paintings', 'pictures', 'particles', 'icons', 'sounds')


def run(assets, only=PARTS, preview=None):
    written = []
    for part in only:
        start = time.time()
        module = __import__(part)
        if part == 'icons':
            out = module.main(assets, os.path.join(preview, 'icons.png') if preview else None)
        else:
            out = module.main(assets)
        out = list(out or [])
        written += out
        print(f'{part}: {len(out)} files ({time.time() - start:.1f} s)')
    return written


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('assets', help='the mod assets folder (src/main/resources/assets/circle_of_inevitability)')
    parser.add_argument('--only', default=','.join(PARTS), help='comma-separated parts: ' + ', '.join(PARTS))
    parser.add_argument('--preview', default=None, help='a folder for preview pictures')
    args = parser.parse_args()
    parts = [p.strip() for p in args.only.split(',') if p.strip()]
    for p in parts:
        if p not in PARTS:
            parser.error(f'unknown part {p}; the parts are {", ".join(PARTS)}')
    if args.preview:
        os.makedirs(args.preview, exist_ok=True)
    run(args.assets, parts, args.preview)
