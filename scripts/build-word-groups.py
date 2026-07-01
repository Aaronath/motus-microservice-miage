#!/usr/bin/env python3
"""Regénère word-groups.json : groupes thématiques uniquement (lexique = Lexique383)."""
import csv
import json
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "backend/dictionary-service/src/main/resources/word-groups.json"
LEXIQUE_CSV = ROOT / "data/lexique-motus-5-9.csv"

CURATED_SECRETS = {
    "m-musique-7": ["MUSIQUE", "MESQUIN", "MIRAGES", "MELANGE", "MEDIANE", "MAJESTE", "MONTAGE", "MARTEAU", "MELODIE", "MARINES", "MOTIVES", "MATIERE"],
    "e-echo-7": ["EXEMPLE", "EPINARD", "ECOUTER", "EFFACER", "ETOILES", "EMPRISE", "ENTRAIN", "ECHANGE", "ELANCER", "EPREUVE", "ENFANTS", "ETERNEL"],
    "p-paroles-7": ["PARTIES", "PLANTES", "PAYSAGE", "PUPITRE", "PASSAGE", "PARFAIT", "PAROLES", "POISSON", "PEINTRE", "PANIQUE", "PILOTES", "PARURES"],
    "c-cuisine-7": ["CUISINE", "CHAMBRE", "CAMIONS", "COULEUR", "CENTRAL", "CAROTTE", "CASCADE", "CULTURE", "COURAGE", "CARTONS", "CAPTURE", "CARESSE"],
    "s-soleil-7": ["SAUVAGE", "SECRETS", "SAVOIRS", "SERIEUX", "SERVICE", "SYSTEME", "STATUES", "SONDAGE", "SOURIRE", "SUBLIME", "SERPENT", "SINCERE"],
    "a-animaux-7": ["ANIMAUX", "ATELIER", "ADAPTER", "ADRESSE", "ALARMES", "AMITIES", "ANXIETE", "APPORTS", "AFFAIRE", "AVENANT", "ALLEGRO", "ANALYSE"],
    "r-riviere-7": ["RIVIERE", "RECETTE", "ROMANCE", "ROCHERS", "RAPPORT", "RELATIF", "RECITER", "REUNION", "RESPECT", "REVEUSE", "ROULANT", "RUMEURS"],
    "t-theatre-7": ["THEATRE", "TRIBUNE", "TENSION", "TOURNEE", "TROUSSE", "TROPHEE", "TOMATES", "TIRAGES", "TERRAIN", "TURBINE", "TALENTS", "TRISTES"],
    "v-voyage-7": ["VOYAGES", "VILLAGE", "VARIETE", "VITESSE", "VALISES", "VACANCE", "VENDEUR", "VIOLONS", "VERDICT", "VICTIME", "VEILLEE", "VITRINE"],
    "b-bonheur-7": ["BATEAUX", "BONHEUR", "BAIGNER", "BAGUETS", "BIZARRE", "BLOUSON", "BORDURE", "BLESSER", "BOISONS", "BRILLER", "BUREAUX", "BALCONS"],
    "l-lumiere-7": ["LEGENDE", "LANGAGE", "LIAISON", "LIBERTE", "LAVANDE", "LOISIRS", "LUMIERE", "LOURDES", "LAITAGE", "LARGENT", "LIONCEA", "LIVRONS"],
    "f-famille-7": ["FAMILLE", "FENETRE", "FACADES", "FIEVRES", "FORMULE", "FRAGILE", "FUTILES", "FLEURON", "FORTUNE", "FRISSON", "FRONTAL", "FACILES"],
    "d-douceur-7": ["DOUCEUR", "DESSERT", "DIAMANT", "DISPUTE", "DOCTEUR", "DOMAINE", "DOUTEUX", "DRESSER", "DURABLE", "DYNAMIE", "DECLINE", "DEFENSE"],
    "g-galerie-7": ["GARDIEN", "GALERIE", "GARAGES", "GESTION", "GLISSER", "GOUTTES", "GRANDIR", "GRATUIT", "GUERRES", "GRAFFES", "GRIMPER", "GOUTIER"],
    "n-nombres-7": ["NOMBRES", "NOTABLE", "NOUVEAU", "NUANCES", "NEGATIF", "NIVEAUX", "NOYADES", "NOURRIR", "NAPPAGE", "NAIVETE", "NATIVES", "NEUTRON"],
    "h-honneur-7": ["HABITER", "HAMEAUX", "HARICOT", "HERITER", "HONNEUR", "HORIZON", "HUMAINS", "HALTERE", "HANTISE", "HACHOIR", "HALEINE", "HARDEUR"],
    "j-justice-7": ["JARDINS", "JUMENTS", "JETABLE", "JOUEURS", "JOINDRE", "JUMEAUX", "JUSTICE", "JOVIALE", "JARDINE", "JOCKEYS", "JOAILLE", "JUGERAI"],
    "o-oranges-7": ["ORANGES", "OISEAUX", "OUVRAGE", "OLIVIER", "OMBRAGE", "ONCTION", "ORIFICE", "OPULENT", "OBLIGES", "OCTOBRE", "ODORANT", "OFFENSE"],
    "i-inspire-7": ["IMPACTS", "INCITER", "INDIQUE", "INJUSTE", "INSPIRE", "INTERNE", "INVITER", "ISOLANT", "IMAGINE", "IMPORTE", "INDIGNE", "INFINIE"],
    "u-urgence-7": ["URGENCE", "UTILISE", "UNIFORM", "USUELLE", "USURPER", "UNANIME", "UNIFIER", "USINAGE", "UTOPIST", "URANIUM", "USURPES", "UNITEES"],
    "mer-ocean-7": ["MARINES", "MARITIM", "MOUILLE", "MARINER", "MARTEAU", "MIRAGES", "MOUSSON", "MORNING", "MARTRES", "MATADOR", "MAUDIRE", "MELOIR"],
    "motus-jeu-7": ["PARTIES", "PAROLES", "PASSAGE", "PUPITRE", "PAYSAGE", "PLANTES", "PARFAIT", "POISSON", "PEINTRE", "PANIQUE", "PILOTES", "PARURES"],
    "motus-mots-7": ["DEVINER", "DOUCEUR", "DESSERT", "DIAMANT", "DISPUTE", "DOCTEUR", "DOMAINE", "DRESSER", "DURABLE", "DYNAMIE", "DECLINE", "DEFENSE"],
    "tech-java-7": ["SERVEUR", "SERVICE", "SYSTEME", "STATUES", "SONDAGE", "SUBLIME", "SINCERE", "SAUVAGE", "SECRETS", "SAVOIRS", "SERIEUX", "SOURIRE"],
    "tech-deploy-7": ["CLUSTER", "CONTENE", "CULTURE", "CENTRAL", "CAPTURE", "CARTONS", "CAMIONS", "CASCADE", "COURAGE", "CUISINE", "CHAMBRE", "COULEUR"],
    "nature-foret-7": ["FAMILLE", "FENETRE", "FACADES", "FORMULE", "FRAGILE", "FUTILES", "FLEURON", "FORTUNE", "FRISSON", "FRONTAL", "FACILES", "FORETS"],
    "nature-rivage-7": ["RIVIERE", "ROCHERS", "ROMANCE", "RAPPORT", "RELATIF", "RECITER", "REUNION", "RESPECT", "ROULANT", "RUMEURS", "REMPART", "REVEUSE"],
}


def normalize(w: str) -> str:
    import unicodedata
    w = unicodedata.normalize("NFD", w.strip())
    w = "".join(c for c in w if not unicodedata.combining(c))
    return w.upper()


def load_lexicon() -> tuple[dict[str, set[str]], dict[tuple[int, str], list[str]]]:
    if not LEXIQUE_CSV.exists():
        sys.exit(f"Lexique manquant : lancez scripts/build-lexique-motus.py ({LEXIQUE_CSV})")
    lexicon: dict[str, set[str]] = defaultdict(set)
    by_len_letter: dict[tuple[int, str], list[str]] = defaultdict(list)
    with LEXIQUE_CSV.open(encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            w = row["word"].strip()
            length = int(row["length"])
            lexicon[str(length)].add(w)
            by_len_letter[(length, w[0])].append(w)
    for k in lexicon:
        lexicon[k] = set(lexicon[k])
    for k in by_len_letter:
        by_len_letter[k] = sorted(by_len_letter[k])
    return dict(lexicon), dict(by_len_letter)


def main() -> None:
    lexicon, by_len_letter = load_lexicon()
    meta = []
    if OUT.exists():
        old = json.loads(OUT.read_text())
        for g in old.get("groups", []):
            meta.append({
                "id": g["id"],
                "name": g["name"],
                "length": g["length"],
                "firstLetter": g["firstLetter"],
            })
    if not meta:
        sys.exit("Aucun groupe dans word-groups.json")

    missing_secrets: list[str] = []
    groups = []
    for g in meta:
        gid = g["id"]
        length = g["length"]
        letter = g["firstLetter"]
        lex_set = lexicon.get(str(length), set())
        secrets = [
            normalize(w) for w in CURATED_SECRETS.get(gid, [])
            if len(normalize(w)) == length and normalize(w) in lex_set
        ]
        for w in CURATED_SECRETS.get(gid, []):
            nw = normalize(w)
            if len(nw) == length and nw not in lex_set and nw.isalpha():
                missing_secrets.append(f"{gid}:{nw}")
        if len(secrets) < 6:
            pool = [w for w in by_len_letter.get((length, letter), []) if w in lex_set]
            secrets = pool[:12]
        guesses = sorted(set(by_len_letter.get((length, letter), [])) | set(secrets))
        groups.append({
            "id": gid,
            "name": g["name"],
            "length": length,
            "firstLetter": letter,
            "secretWords": secrets,
            "guesses": guesses,
        })

    payload = {"groups": groups}
    OUT.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n")
    print(f"Écrit {OUT} ({OUT.stat().st_size // 1024} Ko, groups only)")
    if missing_secrets:
        print(f"Avertissement : {len(missing_secrets)} secrets hors lexique :", missing_secrets[:10])
    g0 = groups[0]
    print(f"Ex. {g0['id']}: {len(g0['secretWords'])} secrets, {len(g0['guesses'])} propositions")


if __name__ == "__main__":
    main()
