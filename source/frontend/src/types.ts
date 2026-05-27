export type CardNetwork = "VISA" | "MASTERCARD" | "JCB" | "UNIONPAY" | "AMEX" | "NAPAS";
export type CardType = "CREDIT" | "DEBIT" | "PREPAID" | "HYBRID";
export type OfferType = "ONLINE" | "OFFLINE" | "BOTH";
export type OfferEligibilityMode = "ALL" | "CARD_IDS" | "CRITERIA";
export type OfferStatus = "UPCOMING" | "ACTIVE" | "ENDED";
export type NotificationChannel = "EMAIL" | "IN_APP";
export type NotificationPriority = "NORMAL" | "HIGH";
export type NotificationSendMode = "IMMEDIATE" | "QUEUED";

export interface AuthResponse {
  accessToken: string;
  accessTokenExpiresAt: string;
  user: UserProfile;
}

export interface UserProfile {
  id: string;
  email: string;
  roles: string[];
  notificationPreferences: {
    emailEnabled: boolean;
    inAppEnabled: boolean;
  };
}

export interface CardBin {
  id: string;
  bin: string;
}

export interface CardProduct {
  id: string;
  productCode: string;
  bins: CardBin[];
  matchedBins: string[];
  issuer: string;
  name: string | null;
  network: CardNetwork;
  tier: number;
  tierLabel: string;
  tierLabelOverride: string | null;
  type: CardType;
  personal: boolean;
  displayName: string;
}

export interface OfferCategory {
  id: string;
  code: string;
  name: string;
  description: string | null;
}

export interface Offer {
  id: string;
  category: OfferCategory;
  merchantName: string;
  offerSummary: string;
  addressDisplay: string;
  addressUrl: string | null;
  startTime: string;
  endTime: string;
  status: OfferStatus;
  offerType: OfferType;
  eligibilityMode: OfferEligibilityMode;
  targetCardProductIds: string[];
  targetIssuers: string[];
  targetNetworks: CardNetwork[];
  targetTier: number | null;
  targetTypes: CardType[];
  targetPersonal: boolean | null;
  createdAt: string;
  updatedAt: string;
}

export interface BusinessEvent {
  eventId: string;
  eventType: string;
  outcome: string;
  entityType: string;
  entityId: string | null;
  actorUserId: string | null;
  occurredAt: string;
  metadata: Record<string, string>;
}

export interface NotificationCampaign {
  id: string;
  title: string;
  body: string;
  priority: NotificationPriority;
  sendMode: NotificationSendMode;
  scheduledFor: string | null;
  offerId: string | null;
  sourceEventId: string | null;
  recipientCount: number;
  deliveryCount: number;
  deliveries: NotificationDelivery[];
}

export interface NotificationDelivery {
  id: string;
  channel: NotificationChannel;
  recipientUserId: string;
  recipientEmail: string | null;
  status: string;
  sentAt: string | null;
  readAt: string | null;
}
