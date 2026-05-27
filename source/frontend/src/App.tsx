import {
  Activity,
  Bell,
  CreditCard,
  Pencil,
  LogIn,
  LogOut,
  Moon,
  Plus,
  RefreshCw,
  Search,
  Sun,
  Tags,
  Ticket,
  Trash2,
  X,
  UserPlus
} from "lucide-react";
import { FormEvent, useEffect, useMemo, useState } from "react";
import { ApiError, apiRequest } from "./api";
import {
  BusinessEvent,
  CardNetwork,
  CardProduct,
  CardType,
  NotificationCampaign,
  NotificationPriority,
  Offer,
  OfferCategory,
  OfferEligibilityMode,
  OfferType,
  UserProfile
} from "./types";

type Tab = "cards" | "categories" | "offers" | "notifications" | "tracking";

const cardNetworks: CardNetwork[] = ["VISA", "MASTERCARD", "JCB", "UNIONPAY", "AMEX", "NAPAS"];
const cardTypes: CardType[] = ["CREDIT", "DEBIT", "PREPAID", "HYBRID"];
const offerTypes: OfferType[] = ["ONLINE", "OFFLINE", "BOTH"];
const eligibilityModes: OfferEligibilityMode[] = ["ALL", "CARD_IDS", "CRITERIA"];
const tierLabels: Record<CardNetwork, string[]> = {
  VISA: ["Classic", "Gold", "Platinum", "Signature", "Infinite", "Infinite Privilege"],
  MASTERCARD: ["Standard", "Gold", "Platinum", "World", "World Elite", "World Legend"],
  JCB: ["Standard", "Gold", "Platinum", "Ultimate", "Ultimate", "The Class"],
  UNIONPAY: ["Standard", "Gold", "Platinum", "Diamond", "Infinite", "Infinite Privilege"],
  AMEX: ["Green", "Gold", "Platinum", "Platinum", "Centurion", "Centurion Black"],
  NAPAS: ["Standard", "Gold", "Platinum", "Tier 4", "Tier 5", "Tier 6"]
};

const emptyCardForm = {
  issuer: "",
  name: "",
  network: "VISA" as CardNetwork,
  tier: 1,
  tierLabelOverride: "",
  type: "CREDIT" as CardType,
  personal: true,
  bins: ""
};

type CardForm = typeof emptyCardForm;

const emptyCategoryForm = {
  name: "",
  description: ""
};

const emptyManualNotificationForm = {
  body: "",
  priority: "NORMAL" as NotificationPriority
};

const emptyOfferForm = {
  categoryId: "",
  merchantName: "",
  offerSummary: "",
  addressDisplay: "",
  addressUrl: "",
  startTime: "2026-05-27T00:00",
  endTime: "2026-06-30T23:59",
  offerType: "BOTH" as OfferType,
  eligibilityMode: "ALL" as OfferEligibilityMode,
  targetCardProductIds: "",
  targetIssuers: "",
  targetNetworks: "VISA",
  targetTier: "2",
  targetTypes: "CREDIT",
  targetPersonal: "true"
};

function App() {
  const [token, setToken] = useState(() => localStorage.getItem("offerwall.accessToken") ?? "");
  const [user, setUser] = useState<UserProfile | null>(null);
  const [email, setEmail] = useState("test.admin@example.com");
  const [password, setPassword] = useState("testpassword");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [authMode, setAuthMode] = useState<"login" | "signup">("login");
  const [tab, setTab] = useState<Tab>("offers");
  const [dark, setDark] = useState(() => localStorage.getItem("offerwall.theme") === "dark");
  const [status, setStatus] = useState("Ready");
  const [toastVisible, setToastVisible] = useState(false);
  const [busy, setBusy] = useState(false);

  const [cards, setCards] = useState<CardProduct[]>([]);
  const [cardMatches, setCardMatches] = useState<CardProduct[]>([]);
  const [cardPrefix, setCardPrefix] = useState("4204");
  const [cardForm, setCardForm] = useState(emptyCardForm);
  const [editingCard, setEditingCard] = useState<CardProduct | null>(null);
  const [cardEditForm, setCardEditForm] = useState(emptyCardForm);

  const [categories, setCategories] = useState<OfferCategory[]>([]);
  const [categoryForm, setCategoryForm] = useState(emptyCategoryForm);

  const [offers, setOffers] = useState<Offer[]>([]);
  const [offerForm, setOfferForm] = useState(emptyOfferForm);
  const [offerKeyword, setOfferKeyword] = useState("");
  const [offerProductSearch, setOfferProductSearch] = useState("");

  const [events, setEvents] = useState<BusinessEvent[]>([]);
  const [campaigns, setCampaigns] = useState<NotificationCampaign[]>([]);
  const [manualNotificationOpen, setManualNotificationOpen] = useState(false);
  const [manualNotificationForm, setManualNotificationForm] = useState(emptyManualNotificationForm);

  const isAdmin = useMemo(() => user?.roles.includes("ADMIN") || user?.roles.includes("EDITOR"), [user]);
  const selectedOfferProducts = useMemo(() => {
    const selectedIds = new Set(splitList(offerForm.targetCardProductIds));
    return cards.filter((card) => selectedIds.has(card.id));
  }, [cards, offerForm.targetCardProductIds]);
  const offerProductMatches = useMemo(() => {
    const query = offerProductSearch.trim().toLowerCase();
    if (!query) {
      return [];
    }
    const selectedIds = new Set(splitList(offerForm.targetCardProductIds));
    return cards
      .filter((card) => !selectedIds.has(card.id) && card.productCode.toLowerCase().includes(query))
      .slice(0, 8);
  }, [cards, offerForm.targetCardProductIds, offerProductSearch]);

  useEffect(() => {
    document.documentElement.dataset.theme = dark ? "dark" : "light";
    localStorage.setItem("offerwall.theme", dark ? "dark" : "light");
  }, [dark]);

  useEffect(() => {
    if (!token) {
      return;
    }
    void loadCurrentUser().catch((error) => handleStartupError(error));
  }, [token]);

  useEffect(() => {
    if (!token || !user) {
      return;
    }
    void loadAll().catch((error) => handleStartupError(error));
  }, [token, user]);

  useEffect(() => {
    if (!token || status === "Ready") {
      return;
    }
    setToastVisible(true);
    const timeout = window.setTimeout(() => setToastVisible(false), 3600);
    return () => window.clearTimeout(timeout);
  }, [status, token]);

  async function run(action: () => Promise<void>, success: string) {
    setBusy(true);
    try {
      await action();
      setStatus(success);
    } catch (error) {
      if (error instanceof ApiError) {
        setStatus(`${error.status}: ${error.message}`);
      } else if (error instanceof Error) {
        setStatus(error.message);
      } else {
        setStatus("Request failed");
      }
    } finally {
      setBusy(false);
    }
  }

  async function loadCurrentUser() {
    const profile = await apiRequest<UserProfile>("/api/users/me", { token });
    setUser(profile);
  }

  function handleStartupError(error: unknown) {
    if (error instanceof ApiError && error.status === 401) {
      localStorage.removeItem("offerwall.accessToken");
      setToken("");
      setUser(null);
      setStatus("Session expired. Log in again.");
      return;
    }

    if (error instanceof ApiError) {
      setStatus(`${error.status}: ${error.message}`);
    } else if (error instanceof Error) {
      setStatus(error.message);
    } else {
      setStatus("Startup request failed");
    }
  }

  async function loadAll() {
    const loads = [
      ["cards", loadCards],
      ["categories", loadCategories],
      ["offers", loadOffers],
      ["tracking", loadTracking],
      ["notifications", loadCampaigns]
    ] as const;
    const results = await Promise.allSettled(loads.map(([, load]) => load()));
    const failed = results
      .map((result, index) => (result.status === "rejected" ? `${loads[index][0]} (${formatError(result.reason)})` : null))
      .filter(Boolean);
    if (failed.length > 0) {
      throw new Error(`Refresh failed: ${failed.join(", ")}`);
    }
  }

  async function submitAuth(event: FormEvent) {
    event.preventDefault();
    await run(async () => {
      const path = authMode === "login" ? "/api/auth/login" : "/api/auth/signup";
      const response = await apiRequest<{ accessToken: string; user: UserProfile }>(path, {
        method: "POST",
        body:
          authMode === "login"
            ? { email, password }
            : { email, password, confirmPassword: confirmPassword || password }
      });
      localStorage.setItem("offerwall.accessToken", response.accessToken);
      setToken(response.accessToken);
      setUser(response.user);
    }, authMode === "login" ? "Logged in" : "Signed up");
  }

  async function logout() {
    await run(async () => {
      await apiRequest<void>("/api/auth/logout", { method: "POST", token });
      localStorage.removeItem("offerwall.accessToken");
      setToken("");
      setUser(null);
      setCards([]);
      setOffers([]);
      setCategories([]);
      setEvents([]);
      setCampaigns([]);
    }, "Logged out");
  }

  async function loadCards() {
    const response = await apiRequest<CardProduct[]>("/api/cards", { token });
    setCards(response);
  }

  async function matchCards() {
    await run(async () => {
      const response = await apiRequest<CardProduct[]>(`/api/cards/matches?prefix=${encodeURIComponent(cardPrefix)}`, {
        token
      });
      setCardMatches(response);
    }, "Card matches loaded");
  }

  async function createCard(event: FormEvent) {
    event.preventDefault();
    await run(async () => {
      await apiRequest<CardProduct>("/api/cards", {
        method: "POST",
        token,
        body: cardFormPayload(cardForm)
      });
      setCardForm(emptyCardForm);
      await loadCards();
    }, "Card created");
  }

  async function deleteCard(cardId: string) {
    await run(async () => {
      await apiRequest<void>(`/api/cards/${cardId}`, { method: "DELETE", token });
      await loadCards();
    }, "Card deleted");
  }

  function openCardEditor(card: CardProduct) {
    setEditingCard(card);
    setCardEditForm(cardToForm(card));
  }

  function closeCardEditor() {
    setEditingCard(null);
    setCardEditForm(emptyCardForm);
  }

  async function updateCard(event: FormEvent) {
    event.preventDefault();
    if (!editingCard) {
      return;
    }
    await run(async () => {
      await apiRequest<CardProduct>(`/api/cards/${editingCard.id}`, {
        method: "PUT",
        token,
        body: cardFormPayload(cardEditForm)
      });
      closeCardEditor();
      await loadCards();
      if (cardMatches.length > 0) {
        const response = await apiRequest<CardProduct[]>(`/api/cards/matches?prefix=${encodeURIComponent(cardPrefix)}`, {
          token
        });
        setCardMatches(response);
      }
    }, "Card updated");
  }

  async function loadCategories() {
    const response = await apiRequest<OfferCategory[]>("/api/offer-categories");
    setCategories(response);
    if (!offerForm.categoryId && response[0]) {
      setOfferForm((current) => ({ ...current, categoryId: response[0].id }));
    }
  }

  async function createCategory(event: FormEvent) {
    event.preventDefault();
    await run(async () => {
      const created = await apiRequest<OfferCategory>("/api/offer-categories", {
        method: "POST",
        token,
        body: { ...categoryForm, description: blankToNull(categoryForm.description) }
      });
      setCategoryForm(emptyCategoryForm);
      setOfferForm((current) => ({ ...current, categoryId: created.id }));
      await loadCategories();
    }, "Category created");
  }

  async function deleteCategory(categoryId: string) {
    await run(async () => {
      await apiRequest<void>(`/api/offer-categories/${categoryId}`, { method: "DELETE", token });
      await loadCategories();
    }, "Category deleted");
  }

  async function loadOffers() {
    const query = offerKeyword ? `?keyword=${encodeURIComponent(offerKeyword)}` : "";
    const response = await apiRequest<Offer[]>(`/api/offers${query}`);
    setOffers(response);
  }

  async function createOffer(event: FormEvent) {
    event.preventDefault();
    await run(async () => {
      const criteriaMode = offerForm.eligibilityMode === "CRITERIA";
      const cardIdsMode = offerForm.eligibilityMode === "CARD_IDS";
      await apiRequest<Offer>("/api/offers", {
        method: "POST",
        token,
        body: {
          categoryId: offerForm.categoryId,
          merchantName: offerForm.merchantName,
          offerSummary: offerForm.offerSummary,
          addressDisplay: offerForm.addressDisplay,
          addressUrl: blankToNull(offerForm.addressUrl),
          startTime: toInstant(offerForm.startTime),
          endTime: toInstant(offerForm.endTime),
          offerType: offerForm.offerType,
          eligibilityMode: offerForm.eligibilityMode,
          targetCardProductIds: cardIdsMode ? splitList(offerForm.targetCardProductIds) : [],
          targetIssuers: criteriaMode ? splitList(offerForm.targetIssuers) : [],
          targetNetworks: criteriaMode ? splitList(offerForm.targetNetworks) : [],
          targetTier: criteriaMode && offerForm.targetTier !== "" ? Number(offerForm.targetTier) : null,
          targetTypes: criteriaMode ? splitList(offerForm.targetTypes) : [],
          targetPersonal: criteriaMode && offerForm.targetPersonal !== "" ? offerForm.targetPersonal === "true" : null
        }
      });
      setOfferForm((current) => ({ ...emptyOfferForm, categoryId: current.categoryId }));
      setOfferProductSearch("");
      await Promise.all([loadOffers(), loadTracking()]);
    }, "Offer created");
  }

  function addOfferProduct(card: CardProduct) {
    const selectedIds = splitList(offerForm.targetCardProductIds);
    if (selectedIds.includes(card.id)) {
      return;
    }
    setOfferForm({ ...offerForm, targetCardProductIds: [...selectedIds, card.id].join(",") });
    setOfferProductSearch("");
  }

  function removeOfferProduct(cardId: string) {
    setOfferForm({
      ...offerForm,
      targetCardProductIds: splitList(offerForm.targetCardProductIds)
        .filter((id) => id !== cardId)
        .join(",")
    });
  }

  async function deleteOffer(offerId: string) {
    await run(async () => {
      await apiRequest<void>(`/api/offers/${offerId}`, { method: "DELETE", token });
      await Promise.all([loadOffers(), loadTracking()]);
    }, "Offer deleted");
  }

  async function loadTracking() {
    const response = await apiRequest<BusinessEvent[]>("/api/business-events?limit=20", { token });
    setEvents(response);
  }

  async function loadCampaigns() {
    const response = await apiRequest<NotificationCampaign[]>("/api/notifications", { token });
    setCampaigns(response);
  }

  function openManualNotification() {
    setManualNotificationForm(emptyManualNotificationForm);
    setManualNotificationOpen(true);
  }

  function closeManualNotification() {
    setManualNotificationOpen(false);
    setManualNotificationForm(emptyManualNotificationForm);
  }

  async function createManualNotification(event: FormEvent) {
    event.preventDefault();
    if (!user) {
      return;
    }
    await run(async () => {
      await apiRequest<NotificationCampaign>("/api/notifications", {
        method: "POST",
        token,
        body: {
          title: "Manual notification",
          body: manualNotificationForm.body.trim(),
          priority: manualNotificationForm.priority,
          sendMode: "IMMEDIATE",
          scheduledFor: null,
          offerId: offers[0]?.id ?? null,
          sourceEventId: null,
          channels: ["IN_APP"],
          recipients: [
            {
              userId: user.id,
              email: user.email,
              emailEnabled: false,
              inAppEnabled: true
            }
          ]
        }
      });
      closeManualNotification();
      await loadCampaigns();
    }, "Notification created");
  }

  async function createOfferEventNotification() {
    await run(async () => {
      const event = offerCreatedEventForNotification(events);
      if (!event) {
        throw new Error("No OFFER_CREATED event found. Create an offer first.");
      }
      try {
        await apiRequest<NotificationCampaign>(`/api/notifications/offer-created-events/${event.eventId}`, {
          method: "POST",
          token,
          body: {
            priority: "NORMAL",
            sendMode: "IMMEDIATE",
            scheduledFor: null,
            channels: ["IN_APP"],
            title: null,
            body: null
          }
        });
      } catch (error) {
        if (error instanceof ApiError && error.status === 400 && error.message.includes("no eligible recipients")) {
          throw new Error("No eligible recipients. Add an eligible card to a user wallet, or create an ALL-eligibility offer for this demo.");
        }
        throw error;
      }
      await loadCampaigns();
    }, "Offer notification created");
  }

  if (!token || !user) {
    return (
      <main className="auth-shell">
        <section className="auth-panel">
          <div>
            <p className="eyebrow">Offerwall Admin</p>
            <h1>{authMode === "login" ? "Log in" : "Sign up"}</h1>
          </div>
          <form onSubmit={submitAuth} className="stack">
            <label>
              Email
              <input value={email} onChange={(event) => setEmail(event.target.value)} type="email" />
            </label>
            <label>
              Password
              <input value={password} onChange={(event) => setPassword(event.target.value)} type="password" />
            </label>
            {authMode === "signup" && (
              <label>
                Confirm password
                <input
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  type="password"
                />
              </label>
            )}
            <button className="primary" type="submit" disabled={busy} title={authMode === "login" ? "Log in" : "Sign up"}>
              {authMode === "login" ? <LogIn size={18} /> : <UserPlus size={18} />}
              {authMode === "login" ? "Log in" : "Sign up"}
            </button>
          </form>
          <button className="text-button" type="button" onClick={() => setAuthMode(authMode === "login" ? "signup" : "login")}>
            {authMode === "login" ? "Create account" : "Use existing account"}
          </button>
          <StatusLine status={status} busy={busy} />
        </section>
        <button className="theme-float" type="button" onClick={() => setDark((value) => !value)} title="Toggle theme">
          {dark ? <Sun size={18} /> : <Moon size={18} />}
        </button>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div>
          <p className="eyebrow">Offerwall</p>
          <h1>Admin</h1>
        </div>
        <nav className="nav-list">
          <NavButton tab="offers" current={tab} setTab={setTab} icon={<Ticket size={18} />} label="Offers" />
          <NavButton tab="categories" current={tab} setTab={setTab} icon={<Tags size={18} />} label="Categories" />
          <NavButton tab="cards" current={tab} setTab={setTab} icon={<CreditCard size={18} />} label="Cards" />
          <NavButton tab="notifications" current={tab} setTab={setTab} icon={<Bell size={18} />} label="Notifications" />
          <NavButton tab="tracking" current={tab} setTab={setTab} icon={<Activity size={18} />} label="Tracking" />
        </nav>
        <div className="user-block">
          <strong>{user.email}</strong>
          <span>{user.roles.join(", ")}</span>
        </div>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div className="topbar-spacer" />
          <div className="topbar-actions">
            <div className="toolbar">
              <button type="button" className="icon-button" onClick={() => void run(loadAll, "Refreshed")} title="Refresh">
                <RefreshCw size={18} />
              </button>
              <button type="button" className="icon-button" onClick={() => setDark((value) => !value)} title="Toggle theme">
                {dark ? <Sun size={18} /> : <Moon size={18} />}
              </button>
              <button type="button" className="icon-button" onClick={() => void logout()} title="Log out">
                <LogOut size={18} />
              </button>
            </div>
            <Toast status={isAdmin ? status : "Admin/editor role required for writes"} busy={busy} visible={toastVisible || busy || !isAdmin} />
          </div>
        </header>

        {tab === "offers" && (
          <section className="grid two">
            <Panel title="Create Offer">
              <form onSubmit={createOffer} className="form-grid">
                <label>
                  Category
                  <select value={offerForm.categoryId} onChange={(event) => setOfferForm({ ...offerForm, categoryId: event.target.value })}>
                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Merchant
                  <input value={offerForm.merchantName} onChange={(event) => setOfferForm({ ...offerForm, merchantName: event.target.value })} />
                </label>
                <label className="wide">
                  Summary
                  <textarea value={offerForm.offerSummary} onChange={(event) => setOfferForm({ ...offerForm, offerSummary: event.target.value })} />
                </label>
                <label>
                  Address
                  <input value={offerForm.addressDisplay} onChange={(event) => setOfferForm({ ...offerForm, addressDisplay: event.target.value })} />
                </label>
                <label>
                  Address URL
                  <input value={offerForm.addressUrl} onChange={(event) => setOfferForm({ ...offerForm, addressUrl: event.target.value })} />
                </label>
                <label>
                  Start
                  <input
                    type="datetime-local"
                    value={offerForm.startTime}
                    onChange={(event) => setOfferForm({ ...offerForm, startTime: event.target.value })}
                  />
                </label>
                <label>
                  End
                  <input type="datetime-local" value={offerForm.endTime} onChange={(event) => setOfferForm({ ...offerForm, endTime: event.target.value })} />
                </label>
                <label>
                  Type
                  <select value={offerForm.offerType} onChange={(event) => setOfferForm({ ...offerForm, offerType: event.target.value as OfferType })}>
                    {offerTypes.map((value) => (
                      <option key={value}>{value}</option>
                    ))}
                  </select>
                </label>
                <label>
                  Eligibility
                  <select
                    value={offerForm.eligibilityMode}
                    onChange={(event) => setOfferForm({ ...offerForm, eligibilityMode: event.target.value as OfferEligibilityMode })}
                  >
                    {eligibilityModes.map((value) => (
                      <option key={value}>{value}</option>
                    ))}
                  </select>
                </label>
                {offerForm.eligibilityMode === "CARD_IDS" && (
                  <div className="wide selector-block">
                    <label>
                      Product code
                      <input
                        value={offerProductSearch}
                        onChange={(event) => setOfferProductSearch(event.target.value)}
                        placeholder="Search product code"
                      />
                    </label>
                    {offerProductMatches.length > 0 && (
                      <ul className="selector-list">
                        {offerProductMatches.map((card) => (
                          <li key={card.id}>
                            <button type="button" onClick={() => addOfferProduct(card)}>
                              <Plus size={16} />
                              <span>{card.productCode}</span>
                            </button>
                          </li>
                        ))}
                      </ul>
                    )}
                    {selectedOfferProducts.length > 0 && (
                      <ul className="chip-list">
                        {selectedOfferProducts.map((card) => (
                          <li key={card.id}>
                            <span>{card.productCode}</span>
                            <button type="button" onClick={() => removeOfferProduct(card.id)} title="Remove product">
                              <Trash2 size={14} />
                            </button>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                )}
                {offerForm.eligibilityMode === "CRITERIA" && (
                  <>
                    <label>
                      Issuers
                      <input value={offerForm.targetIssuers} onChange={(event) => setOfferForm({ ...offerForm, targetIssuers: event.target.value })} />
                    </label>
                    <label>
                      Networks
                      <input value={offerForm.targetNetworks} onChange={(event) => setOfferForm({ ...offerForm, targetNetworks: event.target.value })} />
                    </label>
                    <label>
                      Min tier
                      <input value={offerForm.targetTier} onChange={(event) => setOfferForm({ ...offerForm, targetTier: event.target.value })} />
                    </label>
                    <label>
                      Types
                      <input value={offerForm.targetTypes} onChange={(event) => setOfferForm({ ...offerForm, targetTypes: event.target.value })} />
                    </label>
                    <label>
                      Personal
                      <select value={offerForm.targetPersonal} onChange={(event) => setOfferForm({ ...offerForm, targetPersonal: event.target.value })}>
                        <option value="">Any</option>
                        <option value="true">Yes</option>
                        <option value="false">No</option>
                      </select>
                    </label>
                  </>
                )}
                <button className="primary wide" type="submit" disabled={busy || !isAdmin} title="Create offer">
                  <Plus size={18} />
                  Create offer
                </button>
              </form>
            </Panel>
            <Panel title="Offers">
              <div className="inline">
                <input value={offerKeyword} onChange={(event) => setOfferKeyword(event.target.value)} placeholder="Merchant keyword" />
                <button type="button" className="icon-button" onClick={() => void run(loadOffers, "Offers loaded")} title="Search offers">
                  <Search size={18} />
                </button>
              </div>
              <List>
                {offers.map((offer) => (
                  <li key={offer.id} className="item">
                    <div>
                      <strong>{offer.merchantName}</strong>
                      <span>{offer.category.name} · {offer.status} · {offer.offerType}</span>
                      <small>{offer.offerSummary}</small>
                    </div>
                    <button type="button" className="icon-button danger" onClick={() => void deleteOffer(offer.id)} title="Delete offer">
                      <Trash2 size={18} />
                    </button>
                  </li>
                ))}
              </List>
            </Panel>
          </section>
        )}

        {tab === "categories" && (
          <section className="grid two">
            <Panel title="Create Category">
              <form onSubmit={createCategory} className="stack">
                <label>
                  Name
                  <input value={categoryForm.name} onChange={(event) => setCategoryForm({ ...categoryForm, name: event.target.value })} />
                </label>
                <label>
                  Description
                  <textarea value={categoryForm.description} onChange={(event) => setCategoryForm({ ...categoryForm, description: event.target.value })} />
                </label>
                <button className="primary" type="submit" disabled={busy || !isAdmin} title="Create category">
                  <Plus size={18} />
                  Create category
                </button>
              </form>
            </Panel>
            <Panel title="Categories">
              <List>
                {categories.map((category) => (
                  <li key={category.id} className="item">
                    <div>
                      <strong>{category.name}</strong>
                      <span>{category.code}</span>
                      <small>{category.description}</small>
                    </div>
                    <button type="button" className="icon-button danger" onClick={() => void deleteCategory(category.id)} title="Delete category">
                      <Trash2 size={18} />
                    </button>
                  </li>
                ))}
              </List>
            </Panel>
          </section>
        )}

        {tab === "cards" && (
          <>
            <section className="grid two">
              <Panel title="Create Card">
                <form onSubmit={createCard} className="form-grid">
                  <CardFields form={cardForm} setForm={setCardForm} />
                <button className="primary wide" type="submit" disabled={busy} title="Create card">
                  <Plus size={18} />
                  Create card
                </button>
              </form>
            </Panel>
            <Panel title="Cards">
              <div className="inline">
                <input value={cardPrefix} onChange={(event) => setCardPrefix(event.target.value)} placeholder="BIN prefix" />
                <button type="button" className="icon-button" onClick={() => void matchCards()} title="Match BIN">
                  <Search size={18} />
                </button>
              </div>
              <List>
                {(cardMatches.length > 0 ? cardMatches : cards).map((card) => (
                  <li key={card.id} className="item">
                    <div>
                      <strong>{card.displayName}</strong>
                      <span>{card.network} · tier {apiTierToUiTier(card.tier)} ({card.tierLabel}) · {card.type}</span>
                      <small>{card.bins.map((bin) => bin.bin).join(", ")}</small>
                    </div>
                    <div className="item-actions">
                      <button type="button" className="icon-button" onClick={() => openCardEditor(card)} title="Edit card">
                        <Pencil size={18} />
                      </button>
                      <button type="button" className="icon-button danger" onClick={() => void deleteCard(card.id)} title="Delete card">
                        <Trash2 size={18} />
                      </button>
                    </div>
                  </li>
                ))}
              </List>
            </Panel>
            </section>
            {editingCard && (
              <div className="modal-backdrop" role="presentation">
                <section className="modal" role="dialog" aria-modal="true" aria-labelledby="edit-card-title">
                  <header className="modal-header">
                    <h2 id="edit-card-title">Edit Card</h2>
                    <button type="button" className="icon-button" onClick={closeCardEditor} title="Close">
                      <X size={18} />
                    </button>
                  </header>
                  <form onSubmit={updateCard} className="form-grid">
                    <CardFields form={cardEditForm} setForm={setCardEditForm} />
                    <button className="primary wide" type="submit" disabled={busy} title="Save card">
                      <Pencil size={18} />
                      Save card
                    </button>
                  </form>
                </section>
              </div>
            )}
          </>
        )}

        {tab === "notifications" && (
          <section className="grid two">
            <Panel title="Actions">
              <div className="stack">
                <button type="button" className="primary" onClick={openManualNotification} title="Create manual notification">
                  <Bell size={18} />
                  Manual notification
                </button>
                <button type="button" className="primary" onClick={() => void createOfferEventNotification()} title="Create offer notification">
                  <Bell size={18} />
                  Offer event notification
                </button>
              </div>
            </Panel>
            <Panel title="Campaigns">
              <List>
                {campaigns.map((campaign) => (
                  <li key={campaign.id} className="item">
                    <div>
                      <strong>{campaign.title}</strong>
                      <span>{campaign.priority} · {campaign.sendMode}</span>
                      <small>{campaign.recipientCount} recipients · {campaign.deliveryCount} deliveries</small>
                    </div>
                  </li>
                ))}
              </List>
            </Panel>
            {manualNotificationOpen && (
              <div className="modal-backdrop" role="presentation">
                <section className="modal" role="dialog" aria-modal="true" aria-labelledby="manual-notification-title">
                  <header className="modal-header">
                    <h2 id="manual-notification-title">Manual Notification</h2>
                    <button type="button" className="icon-button" onClick={closeManualNotification} title="Close">
                      <X size={18} />
                    </button>
                  </header>
                  <form onSubmit={createManualNotification} className="stack">
                    <label>
                      Body
                      <textarea
                        required
                        value={manualNotificationForm.body}
                        onChange={(event) => setManualNotificationForm({ ...manualNotificationForm, body: event.target.value })}
                      />
                    </label>
                    <label>
                      Priority
                      <select
                        value={manualNotificationForm.priority}
                        onChange={(event) =>
                          setManualNotificationForm({ ...manualNotificationForm, priority: event.target.value as NotificationPriority })
                        }
                      >
                        <option value="NORMAL">Normal</option>
                        <option value="HIGH">Override</option>
                      </select>
                    </label>
                    <button className="primary" type="submit" disabled={busy || !manualNotificationForm.body.trim()} title="Send notification">
                      <Bell size={18} />
                      Send notification
                    </button>
                  </form>
                </section>
              </div>
            )}
          </section>
        )}

        {tab === "tracking" && (
          <Panel title="Business Events">
            <List>
              {events.map((event) => (
                <li key={event.eventId} className="item">
                  <div>
                    <strong>{event.eventType}</strong>
                    <span>{event.entityType} · {event.outcome}</span>
                    <small>{event.entityId} · {new Date(event.occurredAt).toLocaleString()}</small>
                  </div>
                </li>
              ))}
            </List>
          </Panel>
        )}
      </section>
    </main>
  );
}

function Panel({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="panel">
      <h2>{title}</h2>
      {children}
    </section>
  );
}

function List({ children }: { children: React.ReactNode }) {
  return <ul className="list">{children}</ul>;
}

function NavButton({
  tab,
  current,
  setTab,
  icon,
  label
}: {
  tab: Tab;
  current: Tab;
  setTab: (tab: Tab) => void;
  icon: React.ReactNode;
  label: string;
}) {
  return (
    <button type="button" className={current === tab ? "nav active" : "nav"} onClick={() => setTab(tab)} title={label}>
      {icon}
      {label}
    </button>
  );
}

function StatusLine({ status, busy }: { status: string; busy: boolean }) {
  return (
    <div className="status">
      <span className={busy ? "dot pulse" : "dot"} />
      {status}
    </div>
  );
}

function Toast({ status, busy, visible }: { status: string; busy: boolean; visible: boolean }) {
  if (!visible) {
    return null;
  }

  return (
    <div className="toast" role="status" aria-live="polite">
      <span className={busy ? "dot pulse" : "dot"} />
      {status}
    </div>
  );
}

function CardFields({ form, setForm }: { form: CardForm; setForm: (form: CardForm) => void }) {
  return (
    <>
      <label>
        Issuer
        <input value={form.issuer} onChange={(event) => setForm({ ...form, issuer: event.target.value })} />
      </label>
      <label>
        Name
        <input value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} />
      </label>
      <label>
        Network
        <select
          value={form.network}
          onChange={(event) => {
            const network = event.target.value as CardNetwork;
            setForm({ ...form, network, tier: firstTierForNetwork(network) });
          }}
        >
          {cardNetworks.map((value) => (
            <option key={value}>{value}</option>
          ))}
        </select>
      </label>
      <label>
        Tier
        <select value={form.tier} onChange={(event) => setForm({ ...form, tier: Number(event.target.value) })}>
          {tierOptionsForNetwork(form.network).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </label>
      <label>
        Type
        <select value={form.type} onChange={(event) => setForm({ ...form, type: event.target.value as CardType })}>
          {cardTypes.map((value) => (
            <option key={value}>{value}</option>
          ))}
        </select>
      </label>
      <label>
        Tier label override
        <input value={form.tierLabelOverride} onChange={(event) => setForm({ ...form, tierLabelOverride: event.target.value })} />
      </label>
      <label>
        BINs
        <input value={form.bins} onChange={(event) => setForm({ ...form, bins: event.target.value })} />
      </label>
      <label className="check">
        <input type="checkbox" checked={form.personal} onChange={(event) => setForm({ ...form, personal: event.target.checked })} />
        Personal
      </label>
    </>
  );
}

function splitList(value: string) {
  return value
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function blankToNull(value: string) {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function formatError(error: unknown) {
  if (error instanceof ApiError) {
    return error.status === 0 ? error.message : `${error.status}: ${error.message}`;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return "unknown error";
}

function offerCreatedEventForNotification(events: BusinessEvent[]) {
  const offerEvents = events.filter((event) => event.eventType === "OFFER_CREATED");
  return offerEvents.find((event) => event.metadata?.eligibilityMode === "ALL") ?? offerEvents[0] ?? null;
}

function cardToForm(card: CardProduct): CardForm {
  return {
    issuer: card.issuer,
    name: card.name ?? "",
    network: card.network,
    tier: apiTierToUiTier(card.tier),
    tierLabelOverride: card.tierLabelOverride ?? "",
    type: card.type,
    personal: card.personal,
    bins: card.bins.map((bin) => bin.bin).join(", ")
  };
}

function cardFormPayload(form: CardForm) {
  return {
    ...form,
    name: blankToNull(form.name),
    tier: uiTierToApiTier(form.tier),
    tierLabelOverride: blankToNull(form.tierLabelOverride),
    bins: splitList(form.bins)
  };
}

function tierOptionsForNetwork(network: CardNetwork) {
  return tierLabels[network].map((label, index) => ({
    value: index + 1,
    label
  }));
}

function firstTierForNetwork(network: CardNetwork) {
  return tierOptionsForNetwork(network)[0]?.value ?? 1;
}

function uiTierToApiTier(tier: number) {
  return tier - 1;
}

function apiTierToUiTier(tier: number) {
  return tier + 1;
}

function toInstant(value: string) {
  return new Date(value).toISOString();
}

export default App;
