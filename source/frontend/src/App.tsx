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
  User,
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
  InAppNotification,
  NotificationCampaign,
  NotificationPriority,
  Offer,
  OfferCategory,
  OfferEligibilityMode,
  OfferType,
  WalletCard,
  UserProfile
} from "./types";

type Tab = "cards" | "categories" | "offers" | "notifications" | "tracking";
type UserTab = "wallet" | "offers";
type UserMenu = "account" | "notifications" | null;

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

const emptyWalletDraft = {
  label: "",
  notes: ""
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
  const [route, setRoute] = useState(() => window.location.pathname);
  const isAdminRoute = route.startsWith("/admin");

  useEffect(() => {
    document.title = isAdminRoute ? "Offerwall Admin" : "Offerwall";
  }, [isAdminRoute]);
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
  const [offerIssuerSearch, setOfferIssuerSearch] = useState("");
  const [editingOfferId, setEditingOfferId] = useState<string | null>(null);

  const [adminFilterProduct, setAdminFilterProduct] = useState("");
  const [adminFilterNetwork, setAdminFilterNetwork] = useState("");
  const [adminFilterMinTier, setAdminFilterMinTier] = useState("");
  const [adminFilterIssuer, setAdminFilterIssuer] = useState("");
  const [adminFilterPersonal, setAdminFilterPersonal] = useState("");
  const [expandedAdminOfferId, setExpandedAdminOfferId] = useState<string | null>(null);

  function matchAdminOfferFilters(offer: Offer): boolean {
    if (adminFilterProduct) {
      const product = cards.find((p) => p.id === adminFilterProduct);
      if (!product) return false;
      return isProductEligibleForOffer(product, offer);
    }

    const hasGenericFilters = adminFilterNetwork || adminFilterMinTier !== "" || adminFilterIssuer || adminFilterPersonal !== "";
    if (!hasGenericFilters) return true;

    const matchingProducts = cards.filter((p) => {
      if (adminFilterNetwork && p.network !== adminFilterNetwork) return false;
      if (adminFilterMinTier !== "" && p.tier < parseInt(adminFilterMinTier)) return false;
      if (adminFilterIssuer && !p.issuer.toLowerCase().includes(adminFilterIssuer.toLowerCase())) return false;
      if (adminFilterPersonal !== "" && String(p.personal) !== adminFilterPersonal) return false;
      return true;
    });

    return matchingProducts.some((p) => isProductEligibleForOffer(p, offer));
  }

  const filteredAdminOffers = useMemo(() => {
    return offers.filter(matchAdminOfferFilters);
  }, [offers, adminFilterProduct, adminFilterNetwork, adminFilterMinTier, adminFilterIssuer, adminFilterPersonal, cards]);

  const existingIssuers = useMemo(() => {
    const set = new Set<string>();
    cards.forEach((card) => {
      if (card.issuer) {
        set.add(card.issuer);
      }
    });
    return Array.from(set).sort();
  }, [cards]);

  const selectedOfferIssuers = useMemo(() => {
    return splitList(offerForm.targetIssuers);
  }, [offerForm.targetIssuers]);

  const offerIssuerMatches = useMemo(() => {
    const query = offerIssuerSearch.trim();
    if (!query) {
      return [];
    }
    const queryLower = query.toLowerCase();
    const selectedSet = new Set(selectedOfferIssuers);
    const matches = existingIssuers.filter(
      (issuer) => !selectedSet.has(issuer) && issuer.toLowerCase().includes(queryLower)
    );
    const hasExactMatch = existingIssuers.some((i) => i.toLowerCase() === queryLower);
    if (!hasExactMatch && !selectedSet.has(query)) {
      matches.push(query);
    }
    return matches;
  }, [existingIssuers, selectedOfferIssuers, offerIssuerSearch]);

  const selectedOfferNetworks = useMemo(() => {
    return splitList(offerForm.targetNetworks);
  }, [offerForm.targetNetworks]);

  const availableOfferNetworks = useMemo(() => {
    return cardNetworks.filter((net) => !selectedOfferNetworks.includes(net));
  }, [selectedOfferNetworks]);

  const selectedOfferTypes = useMemo(() => {
    return splitList(offerForm.targetTypes);
  }, [offerForm.targetTypes]);

  const availableOfferTypes = useMemo(() => {
    return cardTypes.filter((type) => !selectedOfferTypes.includes(type));
  }, [selectedOfferTypes]);

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
    const onPopState = () => setRoute(window.location.pathname);
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, []);

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
      if (!isAdminRoute) {
        navigate("/wallet");
      }
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
      if (!isAdminRoute) {
        navigate("/");
      }
    }, "Logged out");
  }

  function navigate(path: string) {
    window.history.pushState({}, "", path);
    setRoute(path);
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
    if (!window.confirm("Are you sure you want to delete this card product? This action cannot be undone.")) {
      return;
    }
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
    if (!window.confirm("Are you sure you want to delete this offer category? All offers inside this category will be affected. This action cannot be undone.")) {
      return;
    }
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
      const body = {
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
      };

      if (editingOfferId) {
        await apiRequest<Offer>(`/api/offers/${editingOfferId}`, {
          method: "PUT",
          token,
          body
        });
        cancelEditOffer();
        await loadOffers();
      } else {
        const created = await apiRequest<Offer>("/api/offers", {
          method: "POST",
          token,
          body
        });
        setOfferForm((current) => ({ ...emptyOfferForm, categoryId: current.categoryId }));
        setOfferProductSearch("");
        setOfferIssuerSearch("");

        const [_, fetchedEvents] = await Promise.all([
          loadOffers(),
          apiRequest<BusinessEvent[]>("/api/business-events?limit=20", { token })
        ]);
        setEvents(fetchedEvents);

        // Automatically trigger notification for the newly created offer
        const offerEvent = fetchedEvents.find((e) => e.eventType === "OFFER_CREATED" && e.entityId === created.id);
        if (offerEvent) {
          try {
            await apiRequest<NotificationCampaign>(`/api/notifications/offer-created-events/${offerEvent.eventId}`, {
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
            await loadCampaigns();
          } catch (error) {
            console.warn("Could not auto-send notification:", error);
          }
        }
      }
    }, editingOfferId ? "Offer updated" : "Offer created");
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

  function addOfferIssuer(issuer: string) {
    const selected = splitList(offerForm.targetIssuers);
    if (selected.includes(issuer)) {
      return;
    }
    setOfferForm({ ...offerForm, targetIssuers: [...selected, issuer].join(",") });
    setOfferIssuerSearch("");
  }

  function removeOfferIssuer(issuer: string) {
    setOfferForm({
      ...offerForm,
      targetIssuers: splitList(offerForm.targetIssuers)
        .filter((i) => i !== issuer)
        .join(",")
    });
  }

  function addOfferNetwork(network: string) {
    const selected = splitList(offerForm.targetNetworks);
    if (selected.includes(network)) {
      return;
    }
    setOfferForm({ ...offerForm, targetNetworks: [...selected, network].join(",") });
  }

  function removeOfferNetwork(network: string) {
    setOfferForm({
      ...offerForm,
      targetNetworks: splitList(offerForm.targetNetworks)
        .filter((n) => n !== network)
        .join(",")
    });
  }

  function addOfferType(type: string) {
    const selected = splitList(offerForm.targetTypes);
    if (selected.includes(type)) {
      return;
    }
    setOfferForm({ ...offerForm, targetTypes: [...selected, type].join(",") });
  }

  function removeOfferType(type: string) {
    setOfferForm({
      ...offerForm,
      targetTypes: splitList(offerForm.targetTypes)
        .filter((t) => t !== type)
        .join(",")
    });
  }

  function startEditOffer(offer: Offer) {
    const startStr = toLocalDateTimeLocalString(offer.startTime);
    const endStr = toLocalDateTimeLocalString(offer.endTime);

    setOfferForm({
      categoryId: offer.category.id,
      merchantName: offer.merchantName,
      offerSummary: offer.offerSummary,
      addressDisplay: offer.addressDisplay,
      addressUrl: offer.addressUrl || "",
      startTime: startStr,
      endTime: endStr,
      offerType: offer.offerType,
      eligibilityMode: offer.eligibilityMode,
      targetCardProductIds: offer.targetCardProductIds.join(","),
      targetIssuers: offer.targetIssuers.join(","),
      targetNetworks: offer.targetNetworks.join(","),
      targetTier: offer.targetTier !== null ? String(offer.targetTier) : "",
      targetTypes: offer.targetTypes.join(","),
      targetPersonal: offer.targetPersonal !== null ? String(offer.targetPersonal) : ""
    });
    setEditingOfferId(offer.id);
  }

  function cancelEditOffer() {
    setOfferForm(emptyOfferForm);
    setEditingOfferId(null);
    setOfferProductSearch("");
    setOfferIssuerSearch("");
  }

  async function deleteOffer(offerId: string) {
    if (!window.confirm("Are you sure you want to delete this offer? This action cannot be undone.")) {
      return;
    }
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
    if (manualNotificationForm.priority === "HIGH") {
      if (!window.confirm("Are you sure you want to send this HIGH priority notification? All matching users will receive it immediately.")) {
        return;
      }
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

  if (!isAdminRoute) {
    return (
      <UserApp
        route={route}
        token={token}
        user={user}
        email={email}
        setEmail={setEmail}
        password={password}
        setPassword={setPassword}
        confirmPassword={confirmPassword}
        setConfirmPassword={setConfirmPassword}
        authMode={authMode}
        setAuthMode={setAuthMode}
        submitAuth={submitAuth}
        logout={logout}
        navigate={navigate}
        dark={dark}
        setDark={setDark}
        status={status}
        busy={busy}
      />
    );
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
            <Panel title={editingOfferId ? "Edit Offer" : "Create Offer"}>
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
                    <div className="wide selector-block">
                      <label>
                        Issuers
                        <input
                          value={offerIssuerSearch}
                          onChange={(event) => setOfferIssuerSearch(event.target.value)}
                          placeholder="Search or add issuer"
                        />
                      </label>
                      {offerIssuerMatches.length > 0 && (
                        <ul className="selector-list">
                          {offerIssuerMatches.map((issuer) => {
                            const isNew = !existingIssuers.includes(issuer);
                            return (
                              <li key={issuer}>
                                <button type="button" onClick={() => addOfferIssuer(issuer)}>
                                  <Plus size={16} />
                                  <span>{issuer} {isNew ? " (Add custom)" : ""}</span>
                                </button>
                              </li>
                            );
                          })}
                        </ul>
                      )}
                      {selectedOfferIssuers.length > 0 && (
                        <ul className="chip-list">
                          {selectedOfferIssuers.map((issuer) => (
                            <li key={issuer}>
                              <span>{issuer}</span>
                              <button type="button" onClick={() => removeOfferIssuer(issuer)} title="Remove issuer">
                                <Trash2 size={14} />
                              </button>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                    <div className="wide selector-block">
                      <label>
                        Networks
                        <select
                          value=""
                          onChange={(event) => {
                            if (event.target.value) {
                              addOfferNetwork(event.target.value);
                            }
                          }}
                        >
                          <option value="">Select network...</option>
                          {availableOfferNetworks.map((net) => (
                            <option key={net} value={net}>{net}</option>
                          ))}
                        </select>
                      </label>
                      {selectedOfferNetworks.length > 0 && (
                        <ul className="chip-list">
                          {selectedOfferNetworks.map((net) => (
                            <li key={net}>
                              <span>{net}</span>
                              <button type="button" onClick={() => removeOfferNetwork(net)} title="Remove network">
                                <Trash2 size={14} />
                              </button>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                    <label>
                      Min tier
                      <select
                        value={offerForm.targetTier}
                        onChange={(event) => setOfferForm({ ...offerForm, targetTier: event.target.value })}
                      >
                        <option value="">Any</option>
                        {[0, 1, 2, 3, 4, 5].map((idx) => (
                          <option key={idx} value={String(idx)}>
                            Tier {idx + 1} ({getTierLabelsForOfferFilters(selectedOfferNetworks, idx)})
                          </option>
                        ))}
                      </select>
                    </label>
                    <label className="check" style={{ alignSelf: "end", minHeight: "38px" }}>
                      <input
                        type="checkbox"
                        checked={offerForm.targetPersonal === "true"}
                        onChange={(event) => setOfferForm({ ...offerForm, targetPersonal: event.target.checked ? "true" : "false" })}
                      />
                      Personal
                    </label>
                    <div className="wide selector-block">
                      <label>
                        Types
                        <select
                          value=""
                          onChange={(event) => {
                            if (event.target.value) {
                              addOfferType(event.target.value);
                            }
                          }}
                        >
                          <option value="">Select type...</option>
                          {availableOfferTypes.map((type) => (
                            <option key={type} value={type}>{type}</option>
                          ))}
                        </select>
                      </label>
                      {selectedOfferTypes.length > 0 && (
                        <ul className="chip-list">
                          {selectedOfferTypes.map((type) => (
                            <li key={type}>
                              <span>{type}</span>
                              <button type="button" onClick={() => removeOfferType(type)} title="Remove type">
                                <Trash2 size={14} />
                              </button>
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  </>
                )}
                {editingOfferId ? (
                  <div className="wide inline" style={{ gap: "10px" }}>
                    <button className="primary" type="submit" disabled={busy || !isAdmin} title="Save changes" style={{ flex: 1 }}>
                      <Pencil size={18} />
                      Save changes
                    </button>
                    <button className="secondary" type="button" onClick={cancelEditOffer} title="Cancel editing" style={{ flex: 1 }}>
                      Cancel
                    </button>
                  </div>
                ) : (
                  <button className="primary wide" type="submit" disabled={busy || !isAdmin} title="Create offer">
                    <Plus size={18} />
                    Create offer
                  </button>
                )}
              </form>
            </Panel>
            <Panel title="Offers">
              <div className="stack" style={{ marginBottom: "12px" }}>
                <div className="inline">
                  <input value={offerKeyword} onChange={(event) => setOfferKeyword(event.target.value)} placeholder="Merchant keyword" />
                  <button type="button" className="icon-button" onClick={() => void run(loadOffers, "Offers loaded")} title="Search offers">
                    <Search size={18} />
                  </button>
                </div>
                <div className="form-grid">
                  <label className="wide">
                    Card Product
                    <select value={adminFilterProduct} onChange={(e) => setAdminFilterProduct(e.target.value)}>
                      <option value="">Any</option>
                      {cards.map((p) => (
                        <option key={p.id} value={p.id}>{p.displayName}</option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Network
                    <select value={adminFilterNetwork} onChange={(e) => setAdminFilterNetwork(e.target.value)} disabled={!!adminFilterProduct}>
                      <option value="">Any</option>
                      {cardNetworks.map((net) => (
                        <option key={net} value={net}>{net}</option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Min Tier
                    <select value={adminFilterMinTier} onChange={(e) => setAdminFilterMinTier(e.target.value)} disabled={!!adminFilterProduct}>
                      <option value="">Any</option>
                      {[0, 1, 2, 3, 4, 5].map((idx) => (
                        <option key={idx} value={String(idx)}>
                          Tier {idx + 1} ({getTierLabelsForIndex(idx)})
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Issuer
                    <input
                      value={adminFilterIssuer}
                      onChange={(e) => setAdminFilterIssuer(e.target.value)}
                      placeholder="e.g. MB, Vietcombank"
                      disabled={!!adminFilterProduct}
                    />
                  </label>
                  <label className="wide">
                    Purpose
                    <select value={adminFilterPersonal} onChange={(e) => setAdminFilterPersonal(e.target.value)} disabled={!!adminFilterProduct}>
                      <option value="">Any</option>
                      <option value="true">Personal</option>
                      <option value="false">Business</option>
                    </select>
                  </label>
                </div>
              </div>
              <List>
                {filteredAdminOffers.map((offer) => {
                  const isExpanded = expandedAdminOfferId === offer.id;
                  return (
                    <li
                      key={offer.id}
                      className={`item ${isExpanded ? "expanded" : ""}`}
                      onClick={() => setExpandedAdminOfferId(isExpanded ? null : offer.id)}
                      style={{ cursor: "pointer", display: "flex", flexDirection: "column", alignItems: "stretch", padding: "12px" }}
                    >
                      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", width: "100%" }}>
                        <div>
                          <strong>{offer.merchantName}</strong>
                          <span>{offer.category.name} · {offer.status} · {offer.offerType} · {formatDate(offer.startTime)} - {formatDate(offer.endTime)}</span>
                          <small>{offer.offerSummary}</small>
                        </div>
                        <div className="item-actions">
                          <button
                            type="button"
                            className="icon-button"
                            onClick={(e) => {
                              e.stopPropagation();
                              startEditOffer(offer);
                            }}
                            title="Edit offer"
                          >
                            <Pencil size={18} />
                          </button>
                          <button
                            type="button"
                            className="icon-button danger"
                            onClick={(e) => {
                              e.stopPropagation();
                              void deleteOffer(offer.id);
                            }}
                            title="Delete offer"
                          >
                            <Trash2 size={18} />
                          </button>
                        </div>
                      </div>
                      {isExpanded && (
                        <div
                          className="offer-eligibility-details"
                          style={{
                            marginTop: "12px",
                            borderTop: "1px solid var(--line)",
                            paddingTop: "12px",
                            fontSize: "13px",
                            color: "var(--text)",
                            display: "grid",
                            gap: "6px"
                          }}
                          onClick={(e) => e.stopPropagation()}
                        >
                          <strong>Eligibility Rules:</strong>
                          {offer.eligibilityMode === "ALL" && (
                            <div>Available for all card products.</div>
                          )}
                          {offer.eligibilityMode === "CARD_IDS" && (
                            <div>
                              <span>Only available for:</span>
                              <ul style={{ margin: "4px 0 0", paddingLeft: "20px" }}>
                                {offer.targetCardProductIds.map((id) => {
                                  const p = cards.find((prod) => prod.id === id);
                                  return <li key={id}>{p ? p.displayName : id}</li>;
                                })}
                              </ul>
                            </div>
                          )}
                          {offer.eligibilityMode === "CRITERIA" && (
                            <div style={{ display: "grid", gap: "4px" }}>
                              <span>Available for cards matching criteria:</span>
                              {offer.targetIssuers.length > 0 && (
                                <div>• Issuers: {offer.targetIssuers.join(", ")}</div>
                              )}
                              {offer.targetNetworks.length > 0 && (
                                <div>• Networks: {offer.targetNetworks.join(", ")}</div>
                              )}
                              {offer.targetTier !== null && (
                                <div>• Minimum Tier: Tier {offer.targetTier + 1} ({getTierLabelsForIndex(offer.targetTier)})</div>
                              )}
                              {offer.targetTypes.length > 0 && (
                                <div>• Card Types: {offer.targetTypes.join(", ")}</div>
                              )}
                              {offer.targetPersonal !== null && (
                                <div>• Card Purpose: {offer.targetPersonal ? "Personal" : "Business"}</div>
                              )}
                            </div>
                          )}
                        </div>
                      )}
                    </li>
                  );
                })}
                {filteredAdminOffers.length === 0 && <div className="empty-state">No matching offers.</div>}
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

function UserApp({
  route,
  token,
  user,
  email,
  setEmail,
  password,
  setPassword,
  confirmPassword,
  setConfirmPassword,
  authMode,
  setAuthMode,
  submitAuth,
  logout,
  navigate,
  dark,
  setDark,
  status,
  busy
}: {
  route: string;
  token: string;
  user: UserProfile | null;
  email: string;
  setEmail: (value: string) => void;
  password: string;
  setPassword: (value: string) => void;
  confirmPassword: string;
  setConfirmPassword: (value: string) => void;
  authMode: "login" | "signup";
  setAuthMode: (value: "login" | "signup") => void;
  submitAuth: (event: FormEvent) => Promise<void>;
  logout: () => Promise<void>;
  navigate: (path: string) => void;
  dark: boolean;
  setDark: (value: boolean | ((value: boolean) => boolean)) => void;
  status: string;
  busy: boolean;
}) {
  const [tab, setTab] = useState<UserTab>(route.startsWith("/offers") ? "offers" : "wallet");
  const [menu, setMenu] = useState<UserMenu>(null);
  const [walletCards, setWalletCards] = useState<WalletCard[]>([]);
  const [products, setProducts] = useState<CardProduct[]>([]);
  const [matches, setMatches] = useState<CardProduct[]>([]);
  const [binPrefix, setBinPrefix] = useState("");
  const [addingCard, setAddingCard] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<CardProduct | null>(null);
  const [walletDraft, setWalletDraft] = useState(emptyWalletDraft);
  const [editingWalletCard, setEditingWalletCard] = useState<WalletCard | null>(null);
  const [editWalletDraft, setEditWalletDraft] = useState(emptyWalletDraft);
  const [offers, setOffers] = useState<Offer[]>([]);
  const [offerKeyword, setOfferKeyword] = useState("");
  const [availableOnly, setAvailableOnly] = useState(false);
  const [filterProduct, setFilterProduct] = useState("");
  const [filterNetwork, setFilterNetwork] = useState("");
  const [filterMinTier, setFilterMinTier] = useState("");
  const [filterIssuer, setFilterIssuer] = useState("");
  const [filterPersonal, setFilterPersonal] = useState("");
  const [notifications, setNotifications] = useState<InAppNotification[]>([]);
  const [preferences, setPreferences] = useState(() => user?.notificationPreferences ?? { emailEnabled: false, inAppEnabled: true });
  const [userStatus, setUserStatus] = useState("Ready");
  const expiredView = route === "/offers/expired";

  useEffect(() => {
    setTab(route.startsWith("/offers") ? "offers" : "wallet");
    if (route === "/login") {
      setAuthMode("login");
    }
    if (route === "/signup") {
      setAuthMode("signup");
    }
  }, [route]);

  useEffect(() => {
    if (!token || !user) {
      return;
    }
    if (route === "/" || route === "/login" || route === "/signup") {
      navigate("/wallet");
    }
    setPreferences(user.notificationPreferences);
    void loadUserData();
  }, [token, user?.id]);

  useEffect(() => {
    if (!addingCard) {
      return;
    }
    setMatches(binPrefix.trim() ? [] : products);
  }, [addingCard, binPrefix, products]);

  async function userRun(action: () => Promise<void>, success: string) {
    try {
      await action();
      setUserStatus(success);
    } catch (error) {
      setUserStatus(formatError(error));
    }
  }

  async function loadUserData() {
    await Promise.all([loadWallet(), loadProducts(), loadOffers(), loadNotifications()]);
  }

  async function loadWallet() {
    const response = await apiRequest<WalletCard[]>("/api/users/me/wallet/cards", { token });
    setWalletCards(response);
  }

  async function loadProducts() {
    const response = await apiRequest<CardProduct[]>("/api/cards", { token });
    setProducts(response);
    if (!binPrefix.trim()) {
      setMatches(response);
    }
  }

  async function loadOffers(availableOverride?: boolean) {
    const isAvailable = availableOverride !== undefined ? availableOverride : availableOnly;
    const query = offerKeyword ? `?keyword=${encodeURIComponent(offerKeyword)}` : "";
    const path = isAvailable ? `/api/offers/available-for-me${query}` : `/api/offers${query}`;
    const response = await apiRequest<Offer[]>(path, { token: isAvailable ? token : undefined });
    setOffers(response);
  }

  async function loadNotifications() {
    if (!user) {
      return;
    }
    const response = await apiRequest<InAppNotification[]>(`/api/notifications/users/${user.id}/in-app`, { token });
    setNotifications(response);
  }

  async function searchCards() {
    await userRun(async () => {
      if (!binPrefix.trim()) {
        setMatches(products);
        return;
      }
      const response = await apiRequest<CardProduct[]>(`/api/cards/matches?prefix=${encodeURIComponent(binPrefix)}`, { token });
      setMatches(response);
    }, "Card list updated");
  }

  function openAddCard() {
    setAddingCard(true);
    setSelectedProduct(null);
    setWalletDraft(emptyWalletDraft);
    setMatches(products);
  }

  function closeAddCard() {
    setAddingCard(false);
    setSelectedProduct(null);
    setWalletDraft(emptyWalletDraft);
  }

  async function saveWalletCard(event: FormEvent) {
    event.preventDefault();
    if (!selectedProduct || !user) {
      return;
    }
    await userRun(async () => {
      const created = await apiRequest<WalletCard>("/api/users/me/wallet/cards", {
        method: "POST",
        token,
        body: { cardProductId: selectedProduct.id }
      });
      saveWalletMeta(user.id, created.id, walletDraft);
      closeAddCard();
      await loadWallet();
    }, "Card saved");
  }

  async function deleteWalletCard(walletCardId: string) {
    if (!user) {
      return;
    }
    if (!window.confirm("Are you sure you want to remove this card from your wallet?")) {
      return;
    }
    await userRun(async () => {
      await apiRequest<void>(`/api/users/me/wallet/cards/${walletCardId}`, { method: "DELETE", token });
      deleteWalletMeta(user.id, walletCardId);
      await loadWallet();
    }, "Card removed");
  }

  function startEditWalletCard(walletCard: WalletCard) {
    if (!user) return;
    const meta = walletMeta(user.id, walletCard.id);
    setEditWalletDraft({
      label: meta.label || "",
      notes: meta.notes || ""
    });
    setEditingWalletCard(walletCard);
  }

  function closeEditWalletCard() {
    setEditingWalletCard(null);
    setEditWalletDraft(emptyWalletDraft);
  }

  async function saveEditWalletCard(event: FormEvent) {
    event.preventDefault();
    if (!editingWalletCard || !user) {
      return;
    }
    await userRun(async () => {
      saveWalletMeta(user.id, editingWalletCard.id, editWalletDraft);
      closeEditWalletCard();
      await loadWallet();
    }, "Card updated");
  }

  async function savePreferences(event: FormEvent) {
    event.preventDefault();
    await userRun(async () => {
      await apiRequest<UserProfile>("/api/users/me/notification-preferences", {
        method: "PATCH",
        token,
        body: preferences
      });
    }, "Preferences saved");
  }

  const activeOffers = offers.filter((offer) => offer.status === "ACTIVE");
  const upcomingOffers = offers.filter((offer) => offer.status === "UPCOMING");
  const expiredOffers = offers.filter((offer) => offer.status === "ENDED");

  function matchOfferFilters(offer: Offer): boolean {
    if (filterProduct) {
      const product = products.find((p) => p.id === filterProduct);
      if (!product) return false;
      return isProductEligibleForOffer(product, offer);
    }

    const hasGenericFilters = filterNetwork || filterMinTier !== "" || filterIssuer || filterPersonal !== "";
    if (!hasGenericFilters) return true;

    const matchingProducts = products.filter((p) => {
      if (filterNetwork && p.network !== filterNetwork) return false;
      if (filterMinTier !== "" && p.tier < parseInt(filterMinTier)) return false;
      if (filterIssuer && !p.issuer.toLowerCase().includes(filterIssuer.toLowerCase())) return false;
      if (filterPersonal !== "" && String(p.personal) !== filterPersonal) return false;
      return true;
    });

    return matchingProducts.some((p) => isProductEligibleForOffer(p, offer));
  }

  const filteredActiveOffers = useMemo(() => {
    return activeOffers.filter(matchOfferFilters);
  }, [activeOffers, filterProduct, filterNetwork, filterMinTier, filterIssuer, filterPersonal, products]);

  const filteredUpcomingOffers = useMemo(() => {
    return upcomingOffers.filter(matchOfferFilters);
  }, [upcomingOffers, filterProduct, filterNetwork, filterMinTier, filterIssuer, filterPersonal, products]);

  const filteredExpiredOffers = useMemo(() => {
    return expiredOffers.filter(matchOfferFilters);
  }, [expiredOffers, filterProduct, filterNetwork, filterMinTier, filterIssuer, filterPersonal, products]);

  if (!token || !user) {
    if (route === "/login" || route === "/signup") {
      return (
        <main className="auth-shell">
          <section className="auth-panel">
            <div>
              <p className="eyebrow">Offerwall</p>
              <h1>{authMode === "login" ? "User log in" : "Create account"}</h1>
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
                  <input value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} type="password" />
                </label>
              )}
              <button className="primary" type="submit" disabled={busy} title={authMode === "login" ? "Log in" : "Sign up"}>
                {authMode === "login" ? <LogIn size={18} /> : <UserPlus size={18} />}
                {authMode === "login" ? "Log in" : "Sign up"}
              </button>
            </form>
            <button
              className="text-button"
              type="button"
              onClick={() => {
                const next = authMode === "login" ? "signup" : "login";
                setAuthMode(next);
                navigate(next === "login" ? "/login" : "/signup");
              }}
            >
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
      <main className="landing-shell">
        <header className="landing-top">
          <div>
            <p className="eyebrow">Offerwall</p>
            <h1>Track cards. Find eligible offers.</h1>
          </div>
          <button className="icon-button" type="button" onClick={() => setDark((value) => !value)} title="Toggle theme">
            {dark ? <Sun size={18} /> : <Moon size={18} />}
          </button>
        </header>
        <section className="landing-main">
          <div className="landing-copy">
            <h2>One wallet for card-linked offers</h2>
            <p>Save card products, browse active deals, and see what fits your wallet.</p>
            <div className="inline">
              <button className="primary" type="button" onClick={() => { setAuthMode("signup"); navigate("/signup"); }}>
                <UserPlus size={18} />
                Sign up
              </button>
              <button className="primary secondary" type="button" onClick={() => { setAuthMode("login"); navigate("/login"); }}>
                <LogIn size={18} />
                Log in
              </button>
            </div>
          </div>
        </section>
        <button className="text-button landing-admin" type="button" onClick={() => navigate("/admin")}>
          Click here for admin panel
        </button>
      </main>
    );
  }



  return (
    <main className="user-shell">
      <header className="user-header">
        <div>
          <p className="eyebrow">Offerwall</p>
          <h1>{tab === "wallet" ? "Wallet" : "Offers"}</h1>
        </div>
        <nav className="inline">
          <button className={tab === "wallet" ? "nav active" : "nav"} type="button" onClick={() => navigate("/wallet")}>
            <CreditCard size={18} />
            Wallet
          </button>
          <button className={tab === "offers" ? "nav active" : "nav"} type="button" onClick={() => navigate("/offers")}>
            <Ticket size={18} />
            Offers
          </button>
        </nav>
        <div className="topbar-actions">
          <div className="toolbar">
            <button className="icon-button" type="button" onClick={() => void userRun(loadUserData, "Refreshed")} title="Refresh">
              <RefreshCw size={18} />
            </button>
            <button className="icon-button" type="button" onClick={() => setDark((value) => !value)} title="Toggle theme">
              {dark ? <Sun size={18} /> : <Moon size={18} />}
            </button>
            <button className="icon-button" type="button" onClick={() => setMenu(menu === "notifications" ? null : "notifications")} title="Notifications">
              <Bell size={18} />
            </button>
            <button className="icon-button" type="button" onClick={() => setMenu(menu === "account" ? null : "account")} title="Profile">
              <User size={18} />
            </button>
            <button className="icon-button" type="button" onClick={() => void logout()} title="Log out">
              <LogOut size={18} />
            </button>
          </div>
          {menu && (
            <section className="account-popover">
              {menu === "account" && (
                <form className="stack" onSubmit={savePreferences}>
                  <strong>{user.email}</strong>
                  <span>{user.roles.join(", ")}</span>
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={preferences.emailEnabled}
                      onChange={(event) => setPreferences({ ...preferences, emailEnabled: event.target.checked })}
                    />
                    Email notifications
                  </label>
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={preferences.inAppEnabled}
                      onChange={(event) => setPreferences({ ...preferences, inAppEnabled: event.target.checked })}
                    />
                    In-app notifications
                  </label>
                  <button className="primary" type="submit">Save</button>
                </form>
              )}
              {menu === "notifications" && (
                <List>
                  {notifications.map((notification) => {
                    const isManual = notification.title === "Manual notification";
                    return (
                      <li className="item" key={notification.deliveryId}>
                        <div>
                          <strong>{isManual ? notification.body : notification.title}</strong>
                          <span>{notification.priority} · {notification.status} · {new Date(notification.sentAt || notification.createdAt).toLocaleString()}</span>
                          {!isManual && <small>{notification.body}</small>}
                        </div>
                      </li>
                    );
                  })}
                  {notifications.length === 0 && (
                    <li className="empty-state">No notifications.</li>
                  )}
                </List>
              )}
            </section>
          )}
        </div>
      </header>

      {tab === "wallet" && (
        <section className="panel">
          <div className="section-head">
            <h2>Saved cards</h2>
            <button className="primary" type="button" onClick={openAddCard}>
              <Plus size={18} />
              Add
            </button>
          </div>
          <List>
            {walletCards.length === 0 && (
              <li className="empty-state">Wallet is empty. Add a card to get started!</li>
            )}
            {walletCards.map((walletCard) => {
              const meta = user ? walletMeta(user.id, walletCard.id) : emptyWalletDraft;
              const product = walletCard.cardProduct || products.find((p) => p.id === walletCard.cardProductId);
              return (
                <li className="item" key={walletCard.id}>
                  <div>
                    <strong>{meta.label || product?.displayName || walletCard.cardProductId}</strong>
                    {meta.label && product && <span>{product.displayName}</span>}
                    {!product && meta.label && <span>{walletCard.cardProductId}</span>}
                    {meta.notes && <small>{meta.notes}</small>}
                  </div>
                  <div className="item-actions">
                    <button className="icon-button" type="button" onClick={() => startEditWalletCard(walletCard)} title="Edit card">
                      <Pencil size={18} />
                    </button>
                    <button className="icon-button danger" type="button" onClick={() => void deleteWalletCard(walletCard.id)} title="Delete card">
                      <Trash2 size={18} />
                    </button>
                  </div>
                </li>
              );
            })}
          </List>
        </section>
      )}

      {tab === "offers" && (
        <section className="stack">
          <Panel title={expiredView ? "Expired Offers" : "Offerwall"}>
            <div className="stack">
              <div className="inline">
                <input value={offerKeyword} onChange={(event) => setOfferKeyword(event.target.value)} placeholder="Merchant keyword" />
                <button className="icon-button" type="button" onClick={() => void userRun(loadOffers, "Offers loaded")} title="Search offers">
                  <Search size={18} />
                </button>
                <label className="check" style={{ margin: 0, whiteSpace: "nowrap" }}>
                  <input
                    type="checkbox"
                    checked={availableOnly}
                    onChange={(event) => {
                      const checked = event.target.checked;
                      setAvailableOnly(checked);
                      void userRun(() => loadOffers(checked), "Offers loaded");
                    }}
                  />
                  Available for me
                </label>
              </div>
              <div className="form-grid" style={{ marginTop: "12px", borderTop: "1px solid var(--line)", paddingTop: "12px" }}>
                <label>
                  Card Product
                  <select value={filterProduct} onChange={(e) => setFilterProduct(e.target.value)}>
                    <option value="">Any</option>
                    {products.map((p) => (
                      <option key={p.id} value={p.id}>{p.displayName}</option>
                    ))}
                  </select>
                </label>
                <label>
                  Network
                  <select value={filterNetwork} onChange={(e) => setFilterNetwork(e.target.value)} disabled={!!filterProduct}>
                    <option value="">Any</option>
                    {cardNetworks.map((net) => (
                      <option key={net} value={net}>{net}</option>
                    ))}
                  </select>
                </label>
                <label>
                  Min Tier
                  <select value={filterMinTier} onChange={(e) => setFilterMinTier(e.target.value)} disabled={!!filterProduct}>
                    <option value="">Any</option>
                    {[0, 1, 2, 3, 4, 5].map((idx) => (
                      <option key={idx} value={idx}>Tier {idx + 1} ({getTierLabelsForIndex(idx)})</option>
                    ))}
                  </select>
                </label>
                <label>
                  Issuer
                  <input
                    value={filterIssuer}
                    onChange={(e) => setFilterIssuer(e.target.value)}
                    placeholder="e.g. MB, Vietcombank"
                    disabled={!!filterProduct}
                  />
                </label>
                <label className="wide">
                  Purpose
                  <select value={filterPersonal} onChange={(e) => setFilterPersonal(e.target.value)} disabled={!!filterProduct}>
                    <option value="">Any</option>
                    <option value="true">Personal</option>
                    <option value="false">Business</option>
                  </select>
                </label>
              </div>
            </div>
          </Panel>
          {expiredView ? (
            <OfferTable title="Expired" offers={filteredExpiredOffers} products={products} />
          ) : (
            <>
              <OfferTable title="Active" offers={filteredActiveOffers} products={products} />
              <OfferTable title="Upcoming" offers={filteredUpcomingOffers} products={products} />
              <button className="text-button" type="button" onClick={() => navigate("/offers/expired")}>
                View expired offers
              </button>
            </>
          )}
        </section>
      )}

      <StatusLine status={userStatus} busy={busy} />

      {addingCard && (
        <div className="modal-backdrop" role="presentation">
          <section className="modal" role="dialog" aria-modal="true" aria-labelledby="add-wallet-card-title">
            <header className="modal-header">
              <h2 id="add-wallet-card-title">Add Card</h2>
              <button className="icon-button" type="button" onClick={closeAddCard} title="Close">
                <X size={18} />
              </button>
            </header>
            <form className="stack" onSubmit={saveWalletCard}>
              <div className="inline">
                <input value={binPrefix} onChange={(event) => setBinPrefix(event.target.value.replace(/\D/g, "").slice(0, 8))} placeholder="BIN search" />
                <button className="icon-button" type="button" onClick={() => void searchCards()} title="Search cards">
                  <Search size={18} />
                </button>
              </div>
              <List>
                {matches.map((product) => (
                  <li key={product.id} className={selectedProduct?.id === product.id ? "item selected" : "item"}>
                    <div>
                      <strong>{product.displayName}</strong>
                      <span>{product.productCode}</span>
                      <small>{product.bins.map((bin) => bin.bin).join(", ")}</small>
                    </div>
                    <button type="button" className="primary" onClick={() => setSelectedProduct(product)}>
                      Select
                    </button>
                  </li>
                ))}
              </List>
              <label>
                Label
                <input value={walletDraft.label} onChange={(event) => setWalletDraft({ ...walletDraft, label: event.target.value })} />
              </label>
              <label>
                Notes
                <textarea value={walletDraft.notes} onChange={(event) => setWalletDraft({ ...walletDraft, notes: event.target.value })} />
              </label>
              <button className="primary" type="submit" disabled={!selectedProduct}>
                Save
              </button>
            </form>
          </section>
        </div>
      )}

      {editingWalletCard && (
        <div className="modal-backdrop" role="presentation">
          <section className="modal" role="dialog" aria-modal="true" aria-labelledby="edit-wallet-card-title">
            <header className="modal-header">
              <h2 id="edit-wallet-card-title">Edit Card Details</h2>
              <button className="icon-button" type="button" onClick={closeEditWalletCard} title="Close">
                <X size={18} />
              </button>
            </header>
            <form className="stack" onSubmit={saveEditWalletCard}>
              <label>
                Label
                <input value={editWalletDraft.label} onChange={(event) => setEditWalletDraft({ ...editWalletDraft, label: event.target.value })} />
              </label>
              <label>
                Notes
                <textarea value={editWalletDraft.notes} onChange={(event) => setEditWalletDraft({ ...editWalletDraft, notes: event.target.value })} />
              </label>
              <button className="primary" type="submit">
                Save
              </button>
            </form>
          </section>
        </div>
      )}
    </main>
  );
}

function getTierLabelsForIndex(index: number): string {
  const list: string[] = [];
  cardNetworks.forEach((net) => {
    const val = tierLabels[net]?.[index];
    if (val && !val.startsWith("Tier") && !list.includes(val)) {
      list.push(val);
    }
  });
  return list.join(" / ");
}

function getTierLabelsForOfferFilters(selectedNetworks: string[], index: number): string {
  const list: string[] = [];
  const networksToUse = selectedNetworks.length > 0 ? selectedNetworks : cardNetworks;
  networksToUse.forEach((net) => {
    const val = tierLabels[net as CardNetwork]?.[index];
    if (val && !val.startsWith("Tier") && !list.includes(val)) {
      list.push(val);
    }
  });
  return list.join(" / ");
}

function isProductEligibleForOffer(product: CardProduct, offer: Offer): boolean {
  if (offer.eligibilityMode === "ALL") {
    return true;
  }
  if (offer.eligibilityMode === "CARD_IDS") {
    return offer.targetCardProductIds.includes(product.id);
  }
  if (offer.eligibilityMode === "CRITERIA") {
    if (offer.targetIssuers.length > 0) {
      const normCardIssuer = product.issuer.trim().toLowerCase();
      const hasIssuerMatch = offer.targetIssuers.some(
        (iss) => iss.trim().toLowerCase() === normCardIssuer
      );
      if (!hasIssuerMatch) return false;
    }
    if (offer.targetNetworks.length > 0 && !offer.targetNetworks.includes(product.network)) {
      return false;
    }
    if (offer.targetTier !== null && product.tier < offer.targetTier) {
      return false;
    }
    if (offer.targetTypes.length > 0 && !offer.targetTypes.includes(product.type)) {
      return false;
    }
    if (offer.targetPersonal !== null && product.personal !== offer.targetPersonal) {
      return false;
    }
    return true;
  }
  return false;
}

function OfferTable({ title, offers, products }: { title: string; offers: Offer[]; products: CardProduct[] }) {
  const [expandedOfferId, setExpandedOfferId] = useState<string | null>(null);

  return (
    <Panel title={title}>
      <div className="table-list">
        <div className="table-row table-head">
          <span>Merchant</span>
          <span>Category</span>
          <span>Type</span>
          <span>Timing</span>
        </div>
        {offers.map((offer) => {
          const isExpanded = expandedOfferId === offer.id;
          return (
            <div
              className={`table-row clickable ${isExpanded ? "expanded" : ""}`}
              key={offer.id}
              onClick={() => setExpandedOfferId(isExpanded ? null : offer.id)}
              style={{ cursor: "pointer" }}
            >
              <strong>{offer.merchantName}</strong>
              <span>{offer.category.name}</span>
              <span>{offer.offerType}</span>
              <span>{offer.status === "UPCOMING" ? `Starts ${formatDate(offer.startTime)}` : `Ends ${formatDate(offer.endTime)}`}</span>
              <small>{offer.offerSummary}</small>
              {isExpanded && (
                <div className="offer-eligibility-details" style={{ gridColumn: "1 / -1", marginTop: "8px", borderTop: "1px solid var(--line)", paddingTop: "8px" }} onClick={(e) => e.stopPropagation()}>
                  <strong>Eligibility Rules:</strong>
                  {offer.eligibilityMode === "ALL" && (
                    <div style={{ marginTop: "4px" }}>Available for all card products.</div>
                  )}
                  {offer.eligibilityMode === "CARD_IDS" && (
                    <div style={{ marginTop: "4px" }}>
                      <span>Only available for:</span>
                      <ul style={{ margin: "4px 0 0", paddingLeft: "20px" }}>
                        {offer.targetCardProductIds.map((id) => {
                          const p = products.find((prod) => prod.id === id);
                          return <li key={id}>{p ? p.displayName : id}</li>;
                        })}
                      </ul>
                    </div>
                  )}
                  {offer.eligibilityMode === "CRITERIA" && (
                    <div style={{ marginTop: "4px", display: "grid", gap: "4px" }}>
                      <span>Available for cards matching criteria:</span>
                      {offer.targetIssuers.length > 0 && (
                        <div>• Issuers: {offer.targetIssuers.join(", ")}</div>
                      )}
                      {offer.targetNetworks.length > 0 && (
                        <div>• Networks: {offer.targetNetworks.join(", ")}</div>
                      )}
                      {offer.targetTier !== null && (
                        <div>• Minimum Tier: Tier {offer.targetTier + 1} ({getTierLabelsForIndex(offer.targetTier)})</div>
                      )}
                      {offer.targetTypes.length > 0 && (
                        <div>• Card Types: {offer.targetTypes.join(", ")}</div>
                      )}
                      {offer.targetPersonal !== null && (
                        <div>• Card Purpose: {offer.targetPersonal ? "Personal" : "Business"}</div>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
        {offers.length === 0 && <div className="empty-state">No offers.</div>}
      </div>
    </Panel>
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

function toLocalDateTimeLocalString(isoString: string): string {
  const date = new Date(isoString);
  const tzOffset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - tzOffset).toISOString().slice(0, 16);
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString();
}

function walletMeta(userId: string, walletCardId: string) {
  return walletMetaStore(userId)[walletCardId] ?? emptyWalletDraft;
}

function saveWalletMeta(userId: string, walletCardId: string, value: typeof emptyWalletDraft) {
  const store = walletMetaStore(userId);
  store[walletCardId] = {
    label: value.label.trim(),
    notes: value.notes.trim()
  };
  localStorage.setItem(walletMetaKey(userId), JSON.stringify(store));
}

function deleteWalletMeta(userId: string, walletCardId: string) {
  const store = walletMetaStore(userId);
  delete store[walletCardId];
  localStorage.setItem(walletMetaKey(userId), JSON.stringify(store));
}

function walletMetaStore(userId: string): Record<string, typeof emptyWalletDraft> {
  const raw = localStorage.getItem(walletMetaKey(userId));
  if (!raw) {
    return {};
  }
  try {
    return JSON.parse(raw) as Record<string, typeof emptyWalletDraft>;
  } catch {
    return {};
  }
}

function walletMetaKey(userId: string) {
  return `offerwall.walletMeta.${userId}`;
}

export default App;
