import React, {useEffect, useRef, useState} from "react";
import {
    Avatar,
    Box,
    Button,
    Container,
    Divider,
    Grid,
    MenuItem,
    Paper,
    Stack,
    TextField,
    Typography
} from "@mui/material";
import PhotoCameraOutlinedIcon from "@mui/icons-material/PhotoCameraOutlined";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import VpnKeyOutlinedIcon from "@mui/icons-material/VpnKeyOutlined";
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {getErrorMessage} from "../api/client";
import {getMyProfile, updateMyPassword, updateMyProfile, uploadMedia} from "../api/games";
import {UserProfile} from "../GeneralComponents/types/UserProfile";
import {ACCEPTED_UPLOAD_IMAGE_TYPES, buildOptimizedUploadFile} from "../GeneralComponents/utils/imageUploads";

const PROFILE_UPDATED_EVENT = 'gamesdb:profile-updated';
const MAX_AVATAR_DIMENSION = 320;
const MAX_OPTIMIZED_AVATAR_BYTES = 900_000;
const MAX_GIF_AVATAR_BYTES = 30 * 1024 * 1024;

function AccountSettingsPage() {
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [displayName, setDisplayName] = useState('');
    const [bio, setBio] = useState('');
    const [favoritePlatform, setFavoritePlatform] = useState('');
    const [avatarUrl, setAvatarUrl] = useState('');
    const [avatarUrlInput, setAvatarUrlInput] = useState('');
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [message, setMessage] = useState('');
    const [severity, setSeverity] = useState<Severity>(Severity.SUCCESS);
    const [savingProfile, setSavingProfile] = useState(false);
    const [savingPassword, setSavingPassword] = useState(false);
    const [uploadingAvatar, setUploadingAvatar] = useState(false);
    const fileInputRef = useRef<HTMLInputElement | null>(null);

    useEffect(() => {
        let isMounted = true;
        getMyProfile()
            .then(response => {
                if (!isMounted) {
                    return;
                }
                setProfile(response);
                setDisplayName(response.displayName || '');
                setBio(response.bio || '');
                setFavoritePlatform(response.favoritePlatform || '');
                setAvatarUrl(response.avatarUrl || '');
                setAvatarUrlInput(response.avatarUrl?.startsWith('http') ? response.avatarUrl : '');
            })
            .catch(error => {
                if (isMounted) {
                    setMessage(getErrorMessage(error));
                    setSeverity(Severity.ERROR);
                }
            });

        return () => {
            isMounted = false;
        };
    }, []);

    async function handleProfileSave() {
        try {
            setSavingProfile(true);
            const savedProfile = await updateMyProfile({
                displayName,
                bio,
                favoritePlatform,
                avatarUrl
            });
            setProfile(savedProfile);
            setAvatarUrl(savedProfile.avatarUrl || '');
            setAvatarUrlInput(savedProfile.avatarUrl?.startsWith('http') ? savedProfile.avatarUrl : '');
            window.dispatchEvent(new CustomEvent(PROFILE_UPDATED_EVENT, {detail: savedProfile}));
            setMessage('Account settings updated.');
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setSavingProfile(false);
        }
    }

    async function handlePasswordSave() {
        try {
            setSavingPassword(true);
            await updateMyPassword(currentPassword, newPassword);
            setCurrentPassword('');
            setNewPassword('');
            setMessage('Password updated successfully.');
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setSavingPassword(false);
        }
    }

    async function handleAvatarUpload(event: React.ChangeEvent<HTMLInputElement>) {
        const file = event.target.files?.[0];
        if (!file) {
            return;
        }

        event.target.value = '';

        if (!ACCEPTED_UPLOAD_IMAGE_TYPES.has(file.type)) {
            setMessage('Please choose a png, jpg, webp, or gif image.');
            setSeverity(Severity.ERROR);
            return;
        }

        try {
            setUploadingAvatar(true);
            const optimizedFile = await buildOptimizedUploadFile(file, {
                maxDimension: MAX_AVATAR_DIMENSION,
                maxBlobBytes: MAX_OPTIMIZED_AVATAR_BYTES,
                maxGifBytes: MAX_GIF_AVATAR_BYTES,
                oversizedGifMessage: 'Animated GIF avatars must be 30 MB or smaller.',
                oversizedImageMessage: 'Profile picture is still too large after compression. Choose a smaller image.'
            });
            const uploadedAsset = await uploadMedia(optimizedFile, 'AVATAR');
            setAvatarUrl(uploadedAsset.url);
            setAvatarUrlInput('');
            setMessage('Profile picture uploaded. Save settings to apply it to your profile.');
            setSeverity(Severity.INFO);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setUploadingAvatar(false);
        }
    }

    const avatarFallback = (displayName || profile?.displayName || profile?.username || 'G').slice(0, 1).toUpperCase();
    const usingUploadedAvatar = avatarUrl.startsWith('/api/media/');

    return (
        <Box sx={{width: '100%', pt: {xs: 10, md: 12}, pb: 6}}>
            <PopupMessage message={message} severity={severity}/>
            <Container maxWidth="lg">
                <Paper
                    sx={{
                        p: {xs: 3, md: 4},
                        borderRadius: '2rem',
                        mb: 4,
                        color: 'white',
                        background: 'linear-gradient(140deg, rgba(28,11,66,0.95), rgba(73,113,212,0.86), rgba(30,170,150,0.82))',
                        boxShadow: '0 30px 90px rgba(20, 8, 45, 0.28)'
                    }}
                >
                    <Typography variant="overline" sx={{letterSpacing: '0.16em', opacity: 0.84}}>
                        Account Studio
                    </Typography>
                    <Typography variant="h3" fontWeight={900} sx={{mb: 1}}>
                        Shape how your profile shows up across the site.
                    </Typography>
                    <Typography sx={{maxWidth: 760, opacity: 0.9}}>
                        Update your public-facing profile picture, display name, bio, and preferred platform. Password updates stay separate so you can change account details without touching credentials.
                    </Typography>
                </Paper>

                <Grid container spacing={3}>
                    <Grid item xs={12} md={4}>
                        <Paper sx={{p: 3, borderRadius: '1.6rem', height: '100%'}}>
                            <Stack spacing={2} alignItems="center">
                                <Avatar
                                    src={avatarUrl || undefined}
                                    sx={{
                                        width: 132,
                                        height: 132,
                                        fontSize: '3rem',
                                        bgcolor: 'secondary.main',
                                        boxShadow: '0 16px 40px rgba(53, 17, 112, 0.24)'
                                    }}
                                >
                                    {avatarFallback}
                                </Avatar>
                                <Typography variant="h5" fontWeight={800}>
                                    {displayName || profile?.displayName || profile?.username || 'Loading'}
                                </Typography>
                                <Typography color="text.secondary" textAlign="center">
                                    {profile?.email || ' '}
                                </Typography>
                                <Button
                                    variant="outlined"
                                    startIcon={<PhotoCameraOutlinedIcon/>}
                                    onClick={() => fileInputRef.current?.click()}
                                    disabled={uploadingAvatar}
                                >
                                    {uploadingAvatar ? 'Uploading…' : 'Upload Picture'}
                                </Button>
                                <Button
                                    variant="text"
                                    color="inherit"
                                    onClick={() => {
                                        setAvatarUrl('');
                                        setAvatarUrlInput('');
                                    }}
                                >
                                    Remove Picture
                                </Button>
                                <input ref={fileInputRef} type="file" accept="image/*" hidden onChange={handleAvatarUpload}/>
                                <Typography variant="body2" color="text.secondary" textAlign="center">
                                    {usingUploadedAvatar
                                        ? 'Uploaded avatar selected. The raw image data is hidden from the form.'
                                        : 'You can upload a picture or link to a hosted image URL.'}
                                </Typography>
                            </Stack>
                        </Paper>
                    </Grid>

                    <Grid item xs={12} md={8}>
                        <Paper sx={{p: 3, borderRadius: '1.6rem'}}>
                            <Stack spacing={2.5}>
                                <Typography variant="h5" fontWeight={800}>Profile Settings</Typography>
                                <Grid container spacing={2}>
                                    <Grid item xs={12} md={6}>
                                        <TextField label="Username" value={profile?.username || ''} fullWidth disabled/>
                                    </Grid>
                                    <Grid item xs={12} md={6}>
                                        <TextField label="Email" value={profile?.email || ''} fullWidth disabled/>
                                    </Grid>
                                    <Grid item xs={12} md={6}>
                                        <TextField
                                            label="Display name"
                                            value={displayName}
                                            onChange={(event) => setDisplayName(event.target.value)}
                                            fullWidth
                                        />
                                    </Grid>
                                    <Grid item xs={12} md={6}>
                                        <TextField
                                            select
                                            label="Favorite platform"
                                            value={favoritePlatform}
                                            onChange={(event) => setFavoritePlatform(event.target.value)}
                                            fullWidth
                                        >
                                            {['', 'PS5', 'PS4', 'PS3', 'PS Vita', 'PSP'].map(option => (
                                                <MenuItem key={option || 'none'} value={option}>
                                                    {option || 'Not set'}
                                                </MenuItem>
                                            ))}
                                        </TextField>
                                    </Grid>
                                    <Grid item xs={12}>
                                        <TextField
                                            label="Hosted avatar URL"
                                            value={avatarUrlInput}
                                            onChange={(event) => {
                                                const nextValue = event.target.value;
                                                setAvatarUrlInput(nextValue);
                                                setAvatarUrl(nextValue.trim());
                                            }}
                                            fullWidth
                                            placeholder="https://example.com/avatar.png"
                                            helperText={usingUploadedAvatar
                                                ? 'A local upload is selected right now. Pasting a URL here will replace it.'
                                                : 'Optional. Paste a hosted image URL or use Upload Picture above. Animated GIF uploads can be larger than static images.'}
                                        />
                                    </Grid>
                                    <Grid item xs={12}>
                                        <TextField
                                            label="Bio"
                                            value={bio}
                                            onChange={(event) => setBio(event.target.value)}
                                            multiline
                                            minRows={4}
                                            fullWidth
                                        />
                                    </Grid>
                                </Grid>

                                <Box>
                                    <Button
                                        variant="contained"
                                        startIcon={<SaveOutlinedIcon/>}
                                        onClick={handleProfileSave}
                                        disabled={savingProfile}
                                    >
                                        {savingProfile ? 'Saving profile' : 'Save profile'}
                                    </Button>
                                </Box>
                            </Stack>
                        </Paper>

                        <Paper sx={{p: 3, borderRadius: '1.6rem', mt: 3}}>
                            <Stack spacing={2.5}>
                                <Typography variant="h5" fontWeight={800}>Security</Typography>
                                <Divider/>
                                <Grid container spacing={2}>
                                    <Grid item xs={12} md={6}>
                                        <TextField
                                            label="Current password"
                                            type="password"
                                            value={currentPassword}
                                            onChange={(event) => setCurrentPassword(event.target.value)}
                                            fullWidth
                                        />
                                    </Grid>
                                    <Grid item xs={12} md={6}>
                                        <TextField
                                            label="New password"
                                            type="password"
                                            value={newPassword}
                                            onChange={(event) => setNewPassword(event.target.value)}
                                            fullWidth
                                        />
                                    </Grid>
                                </Grid>

                                <Box>
                                    <Button
                                        variant="outlined"
                                        startIcon={<VpnKeyOutlinedIcon/>}
                                        onClick={handlePasswordSave}
                                        disabled={savingPassword}
                                    >
                                        {savingPassword ? 'Updating password' : 'Update password'}
                                    </Button>
                                </Box>
                            </Stack>
                        </Paper>
                    </Grid>
                </Grid>
            </Container>
        </Box>
    );
}

export default AccountSettingsPage;
