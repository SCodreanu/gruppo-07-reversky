import Image from "next/image";
import { PublicHeader } from "@/components/brand";
import { SiteFooter } from "@/components/site-footer";
import styles from "./page.module.css";

const theories = [
  {
    number: "01",
    name: "Theory of the Wound",
    mark: styles.theoryMarkWound,
    description: "Something beyond the world has scratched it open. The Reverse Rain may be the way it bleeds.",
  },
  {
    number: "02",
    name: "Theory of the Invitation",
    mark: styles.theoryMarkInvitation,
    description: "Someone once asked for something to come. Now, at last, something has answered.",
  },
  {
    number: "03",
    name: "Theory of the Archive",
    mark: styles.theoryMarkArchive,
    description: "The Reverse is the world's memory. The Rifts are memories demanding to be lived again.",
  },
  {
    number: "04",
    name: "Theory of the Dream",
    mark: styles.theoryMarkDream,
    description: "Perhaps we are the Reverse, and the ordinary world is only a dream beyond the rain.",
  },
];

export default function PublicEntryPage() {
  return (
    <div className={styles.page}>
      <PublicHeader active="register" />
      <main className={styles.content}>
        <div className={styles.main}>
          <section className={styles.hero} aria-labelledby="landing-title">
            <Image
              className={styles.heroBanner}
              src="/images/reverse-rain-banner.png"
              alt=""
              fill
              sizes="100vw"
              priority
            />
            <div className={styles.heroContent}>
              <p className="eyebrow">The veil is lifting</p>
              <h1 id="landing-title" className={`display-type ${styles.title}`}>
                The darkest stories deserve to be <em>played.</em>
              </h1>
              <p className={styles.intro}>
                Step through the veil, collect cards, and discover the four Theories that shape Reversky.
                Every encounter can change the story.
              </p>
              <div className={styles.heroDetails}>
                <span className={styles.detailLine} aria-hidden="true" />
                <span>A collectible card game</span>
              </div>
              <div className={styles.heroActions}>
                <a className="button" href="/register">Create your account <span aria-hidden="true">→</span></a>
                <a className={styles.heroExplore} href="#game">Explore the game <span aria-hidden="true">↘</span></a>
                <a className={styles.heroExplore} href="#lore">Discover the lore <span aria-hidden="true">↘</span></a>
              </div>
            </div>
            <Image
              className={styles.heroMascot}
              src="/images/reversky-mascot-calm.png"
              alt="Reversky’s purple mascot floats beside the Rift."
              width={1090}
              height={2160}
              sizes="(max-width: 520px) 40vw, 320px"
              priority
            />
          </section>
        </div>

        <section className={styles.gameSection} id="game" aria-labelledby="game-title">
          <div className={styles.gameHeading}>
            <div>
              <p className="eyebrow">The game</p>
              <h2 id="game-title" className={`display-type ${styles.sectionTitle}`}>
                Build your deck. Let the clash unfold.
              </h2>
            </div>
            <p className={styles.sectionLead}>
              Reversky is a digital horror card game where matches play out automatically. Build a deck with a plan,
              challenge another player, and watch every creature and effect resolve.
            </p>
          </div>

          <div className={styles.gameGrid}>
            <article className={`${styles.gameCard} ${styles.deckCard}`}>
              <div className={styles.cardMeta}><span>01</span><span>Build a deck</span></div>
              <h3 className="display-type">Thirty cards. One plan.</h3>
              <p>Every deck brings a precise mix of creatures and support cards into the match.</p>
              <div className={styles.deckCounts} aria-label="Deck composition">
                <span><strong>20</strong><small>Creatures</small></span>
                <span><strong>8</strong><small>Spells or Hidden</small></span>
                <span><strong>1</strong><small>Item</small></span>
                <span><strong>1</strong><small>Terrain</small></span>
              </div>
            </article>

            <article className={styles.gameCard}>
              <div className={styles.cardMeta}><span>02</span><span>Watch the battle</span></div>
              <h3 className="display-type">The match plays itself.</h3>
              <p>
                There is no hand or turn-by-turn play. Once a challenge is accepted, the server reveals cards in order.
                Creatures strike together while Spells, Hidden cards, Items, and Terrain shape the fight.
              </p>
              <div className={styles.cardNote}>
                <span aria-hidden="true">✦</span> Damage stays with a creature, lowering its DEF.
              </div>
            </article>

            <article className={styles.gameCard}>
              <div className={styles.cardMeta}><span>03</span><span>Know your theory</span></div>
              <h3 className="display-type">Every faction has a rival.</h3>
              <p>
                Four Theories shape the creatures and their matchups. A faction can gain an attack edge against one
                rival and lose ground to another.
              </p>
              <a className={styles.cardLink} href="#lore">Meet the four Theories <span aria-hidden="true">↘</span></a>
            </article>
          </div>
        </section>

        <section className={styles.loreSection} id="lore" aria-labelledby="lore-title">
          <div className={styles.loreHeading}>
            <div>
              <p className="eyebrow">The lore</p>
              <h2 id="lore-title" className={`display-type ${styles.sectionTitle}`}>The rain began to rise.</h2>
            </div>
            <p className={styles.loreLead}>
              The world did not end with a war or an explosion. One ordinary afternoon, rain began to lift from the
              ground into a cloudless sky. Its drops last about eleven minutes. Rifts open, and something crosses over.
            </p>
          </div>

          <div className={styles.loreNote}>
            <div className={styles.loreNoteCopy}>
              <span>WHAT CROSSES OVER</span>
              <p>The things that emerge from the Rifts are not monsters in the usual sense. They are consequences.</p>
            </div>
            <Image
              className={styles.loreMascot}
              src="/images/reversky-mascot-alert.png"
              alt=""
              width={1090}
              height={2160}
              sizes="(max-width: 520px) 120px, 180px"
            />
          </div>

          <div className={styles.theoryGrid}>
            {theories.map((theory) => (
              <article className={styles.theoryCard} key={theory.name}>
                <div className={styles.theoryTop}>
                  <span className={styles.theoryNumber}>{theory.number} / THEORY</span>
                  <span className={`${styles.theoryMark} ${theory.mark}`} aria-hidden="true" />
                </div>
                <h3 className="display-type">{theory.name}</h3>
                <p>{theory.description}</p>
              </article>
            ))}
          </div>
        </section>

        <section className={styles.finalCta} aria-labelledby="final-cta-title">
          <p className="eyebrow">The first match is yours to prepare</p>
          <h2 id="final-cta-title" className="display-type">
            Choose your cards. Find out what waits beyond the veil.
          </h2>
          <a className="button" href="/register">Create your account <span aria-hidden="true">→</span></a>
        </section>
      </main>

      <SiteFooter />
    </div>
  );
}
