import React, {useEffect, useRef, useState} from "react";
import {
    Autocomplete,
    Box,
    Button,
    Chip,
    Container,
    Paper,
    Stack,
    TextField,
    Typography
} from "@mui/material";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import PhotoCameraOutlinedIcon from "@mui/icons-material/PhotoCameraOutlined";
import SendOutlinedIcon from "@mui/icons-material/SendOutlined";
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {GameSubmission} from "../GeneralComponents/types/GameSubmission";
import {getErrorMessage} from "../api/client";
import {getMyGameSubmissions, submitGameSuggestion, updateMyGameSubmission, uploadMedia, withdrawMyGameSubmission} from "../api/games";
import {COMMON_PLATFORM_OPTIONS} from "../GeneralComponents/utils/platforms";
import {ACCEPTED_UPLOAD_IMAGE_TYPES, buildOptimizedUploadFile} from "../GeneralComponents/utils/imageUploads";

type SubmissionFormState = {
    name: string;
    providerName: string;
    platforms: string;
    categories: string;
    description: string;
    releaseDate: string;
    imageUrl: string;
    url: string;
};

const EMPTY_FORM: SubmissionFormState = {
    name: '',
    providerName: '',
    platforms: '',
    categories: '',
    description: '',
    releaseDate: '',
    imageUrl: '',
    url: ''
};
const MAX_SUBMISSION_IMAGE_DIMENSION = 960;
const MAX_SUBMISSION_IMAGE_BYTES = 2_500_000;
const MAX_SUBMISSION_GIF_BYTES = 30 * 1024 * 1024;

function parsePlatforms(platforms?: string | null): string[] {
    return (platforms || '')
        .split(',')
        .map(platform => platform.trim())
        .filter(Boolean);
}

function formatDateTime(dateValue?: string | null) {
    if (!dateValue) {
        return 'Unknown';
    }

    const date = new Date(dateValue);
    if (Number.isNaN(date.getTime())) {
        return dateValue;
    }

    return date.toLocaleString();
}

function statusChipColor(status?: string | null): "default" | "success" | "warning" | "error" {
    switch ((status || '').toUpperCase()) {
        case 'APPROVED':
            return 'success';
        case 'REJECTED':
            return 'error';
        case 'WITHDRAWN':
            return 'default';
        default:
            return 'warning';
    }
}

function SubmitGamePage() {
    const [form, setForm] = useState<SubmissionFormState>(EMPTY_FORM);
    const [selectedPlatforms, setSelectedPlatforms] = useState<string[]>([]);
    const [platformInputValue, setPlatformInputValue] = useState('');
    const [imageUrlInput, setImageUrlInput] = useState('');
    const [submissions, setSubmissions] = useState<GameSubmission[]>([]);
    const [submitting, setSubmitting] = useState(false);
    const [uploadingImage, setUploadingImage] = useState(false);
    const [editingSubmissionId, setEditingSubmissionId] = useState<string | null>(null);
    const [message, setMessage] = useState('');
    const [severity, setSeverity] = useState<Severity>(Severity.SUCCESS);
    const fileInputRef = useRef<HTMLInputElement | null>(null);

    function resetFormState() {
        setForm(EMPTY_FORM);
        setSelectedPlatforms([]);
        setPlatformInputValue('');
        setImageUrlInput('');
        setEditingSubmissionId(null);
    }

    async function loadSubmissions() {
        try {
            setSubmissions(await getMyGameSubmissions());
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        }
    }

    useEffect(() => {
        loadSubmissions().catch(() => null);
    }, []);

    async function handleSubmit(event: React.FormEvent) {
        event.preventDefault();
        try {
            const normalizedPlatforms = Array.from(new Set([
                ...selectedPlatforms,
                platformInputValue
            ].map(platform => platform.trim()).filter(Boolean)));

            if (normalizedPlatforms.length === 0) {
                setMessage('Choose at least one platform before submitting.');
                setSeverity(Severity.ERROR);
                return;
            }

            setSubmitting(true);
            const payload = {
                ...form,
                platforms: normalizedPlatforms.join(', ')
            };

            if (editingSubmissionId) {
                await updateMyGameSubmission(editingSubmissionId, payload);
                setMessage('Game suggestion updated and sent back for admin review.');
            } else {
                await submitGameSuggestion(payload);
                setMessage('Game suggestion submitted for admin review.');
            }

            resetFormState();
            setSeverity(Severity.SUCCESS);
            await loadSubmissions();
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setSubmitting(false);
        }
    }

    async function handleImageUpload(event: React.ChangeEvent<HTMLInputElement>) {
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
            setUploadingImage(true);
            const optimizedFile = await buildOptimizedUploadFile(file, {
                maxDimension: MAX_SUBMISSION_IMAGE_DIMENSION,
                maxBlobBytes: MAX_SUBMISSION_IMAGE_BYTES,
                maxGifBytes: MAX_SUBMISSION_GIF_BYTES,
                oversizedGifMessage: 'Animated game image uploads must be 30 MB or smaller.',
                oversizedImageMessage: 'Game image is still too large after compression. Choose a smaller image.'
            });
            const uploadedAsset = await uploadMedia(optimizedFile, 'GAME_SUBMISSION');
            setForm(previous => ({...previous, imageUrl: uploadedAsset.url}));
            setImageUrlInput('');
            setMessage('Game image uploaded. Submit the form to send it for review.');
            setSeverity(Severity.INFO);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setUploadingImage(false);
        }
    }

    function beginEditingSubmission(submission: GameSubmission) {
        setEditingSubmissionId(submission.id);
        setForm({
            name: submission.name || '',
            providerName: submission.providerName || '',
            platforms: submission.platforms || '',
            categories: submission.categories || '',
            description: submission.description || '',
            releaseDate: submission.releaseDate || '',
            imageUrl: submission.imageUrl || '',
            url: submission.url || ''
        });
        setSelectedPlatforms(parsePlatforms(submission.platforms));
        setPlatformInputValue('');
        setImageUrlInput(submission.imageUrl?.startsWith('http') ? submission.imageUrl : '');
        window.scrollTo({top: 0, behavior: 'smooth'});
    }

    async function handleWithdrawSubmission(submissionId: string) {
        if (!window.confirm('Withdraw this game submission from admin review?')) {
            return;
        }

        try {
            await withdrawMyGameSubmission(submissionId);
            if (editingSubmissionId === submissionId) {
                resetFormState();
            }
            await loadSubmissions();
            setMessage('Submission withdrawn.');
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        }
    }

    const usingUploadedImage = form.imageUrl.startsWith('/api/media/');

    return (
        <Box sx={{width: '100%', pt: {xs: 10, md: 12}, pb: 6}}>
            <Container maxWidth="lg">
                <PopupMessage message={message} severity={severity}/>
                <Stack spacing={3}>
                    <Paper
                        component="form"
                        onSubmit={handleSubmit}
                        sx={{
                            p: {xs: 3, md: 4},
                            borderRadius: '2rem',
                            background: 'linear-gradient(180deg, rgba(255,255,255,0.98), rgba(244,248,255,0.94))',
                            boxShadow: '0 24px 70px rgba(15, 23, 42, 0.12)'
                        }}
                    >
                        <Stack spacing={3}>
                            <Box>
                                <Typography variant="h3" fontWeight={900}>
                                    {editingSubmissionId ? 'Update Game Suggestion' : 'Suggest a Game'}
                                </Typography>
                                <Typography color="text.secondary" sx={{mt: 1}}>
                                    {editingSubmissionId
                                        ? 'Revise your request and send it back through admin review.'
                                        : 'Submit a missing title for admin review. Include the best source data you have so it can be approved faster.'}
                                </Typography>
                            </Box>

                            <Stack direction={{xs: 'column', md: 'row'}} spacing={2}>
                                <TextField
                                    fullWidth
                                    required
                                    label="Game name"
                                    value={form.name}
                                    onChange={(event) => setForm(previous => ({...previous, name: event.target.value}))}
                                />
                                <TextField
                                    fullWidth
                                    label="Publisher / studio"
                                    value={form.providerName}
                                    onChange={(event) => setForm(previous => ({...previous, providerName: event.target.value}))}
                                />
                            </Stack>

                            <Stack direction={{xs: 'column', md: 'row'}} spacing={2}>
                                <Autocomplete
                                    fullWidth
                                    multiple
                                    freeSolo
                                    autoSelect
                                    clearOnBlur
                                    selectOnFocus
                                    handleHomeEndKeys
                                    options={COMMON_PLATFORM_OPTIONS}
                                    value={selectedPlatforms}
                                    inputValue={platformInputValue}
                                    onInputChange={(_event, value) => setPlatformInputValue(value)}
                                    onChange={(_event, value) => {
                                        const normalizedPlatforms = Array.from(new Set(value.map(platform => platform.trim()).filter(Boolean)));
                                        setSelectedPlatforms(normalizedPlatforms);
                                        setForm(previous => ({...previous, platforms: normalizedPlatforms.join(', ')}));
                                    }}
                                    renderInput={(params) => (
                                        <TextField
                                            {...params}
                                            label="Platforms"
                                            placeholder="Choose one or more platforms"
                                            helperText="Pick one or more platforms. If you type a custom one, click it, press Enter, or just submit."
                                        />
                                    )}
                                />
                                <TextField
                                    fullWidth
                                    label="Categories / genres"
                                    placeholder="Action, Adventure"
                                    value={form.categories}
                                    onChange={(event) => setForm(previous => ({...previous, categories: event.target.value}))}
                                />
                            </Stack>

                            <TextField
                                fullWidth
                                multiline
                                minRows={5}
                                label="Description"
                                value={form.description}
                                onChange={(event) => setForm(previous => ({...previous, description: event.target.value}))}
                            />

                            <Stack direction={{xs: 'column', md: 'row'}} spacing={2}>
                                <TextField
                                    fullWidth
                                    label="Release date"
                                    type="date"
                                    value={form.releaseDate}
                                    onChange={(event) => setForm(previous => ({...previous, releaseDate: event.target.value}))}
                                    InputLabelProps={{shrink: true}}
                                    helperText="Use the calendar picker or type a date."
                                />
                            </Stack>

                            <Stack direction={{xs: 'column', lg: 'row'}} spacing={2.5} alignItems={{xs: 'stretch', lg: 'center'}}>
                                <TextField
                                    fullWidth
                                    label="Hosted image URL"
                                    value={imageUrlInput}
                                    onChange={(event) => {
                                        const nextValue = event.target.value;
                                        setImageUrlInput(nextValue);
                                        setForm(previous => ({...previous, imageUrl: nextValue.trim()}));
                                    }}
                                    placeholder="https://example.com/game-cover.jpg"
                                    helperText={usingUploadedImage
                                        ? 'A local upload is selected right now. Pasting a URL here will replace it.'
                                        : 'Optional. Paste a hosted cover URL or upload an image from your device.'}
                                />

                                <Stack spacing={1.25} sx={{minWidth: {lg: 280}}}>
                                    <Stack direction={{xs: 'column', sm: 'row', lg: 'column'}} spacing={1.25}>
                                        <Button
                                            variant="outlined"
                                            startIcon={<PhotoCameraOutlinedIcon/>}
                                            onClick={() => fileInputRef.current?.click()}
                                            disabled={uploadingImage}
                                        >
                                            {uploadingImage ? 'Uploading image…' : 'Upload local image'}
                                        </Button>
                                        <Button
                                            variant="text"
                                            color="inherit"
                                            startIcon={<DeleteOutlineIcon/>}
                                            onClick={() => {
                                                setForm(previous => ({...previous, imageUrl: ''}));
                                                setImageUrlInput('');
                                            }}
                                        >
                                            Remove image
                                        </Button>
                                    </Stack>
                                    <input ref={fileInputRef} type="file" accept="image/*" hidden onChange={handleImageUpload}/>
                                    <Typography variant="body2" color="text.secondary">
                                        {usingUploadedImage
                                            ? 'Uploaded cover art selected. The raw image data stays hidden from the text field.'
                                            : 'PNG, JPG, WEBP, and GIF are supported. Static images are optimized before submit.'}
                                    </Typography>
                                </Stack>
                            </Stack>

                            {form.imageUrl && (
                                <Box
                                    sx={{
                                        overflow: 'hidden',
                                        borderRadius: '1.5rem',
                                        border: '1px solid rgba(148, 163, 184, 0.24)',
                                        background: 'rgba(241, 245, 249, 0.78)'
                                    }}
                                >
                                    <Box
                                        component="img"
                                        src={form.imageUrl}
                                        alt={`${form.name || 'Suggested game'} cover preview`}
                                        sx={{
                                            display: 'block',
                                            width: '100%',
                                            maxHeight: 320,
                                            objectFit: 'cover'
                                        }}
                                    />
                                </Box>
                            )}

                            <TextField
                                fullWidth
                                label="Store / reference URL"
                                value={form.url}
                                onChange={(event) => setForm(previous => ({...previous, url: event.target.value}))}
                            />

                            <Box>
                                <Stack direction={{xs: 'column', sm: 'row'}} spacing={1.5}>
                                    <Button type="submit" variant="contained" endIcon={<SendOutlinedIcon/>} disabled={submitting}>
                                        {submitting
                                            ? (editingSubmissionId ? 'Saving changes' : 'Submitting')
                                            : (editingSubmissionId ? 'Save and resubmit' : 'Submit for approval')}
                                    </Button>
                                    {editingSubmissionId && (
                                        <Button variant="outlined" onClick={resetFormState} disabled={submitting}>
                                            Cancel edit
                                        </Button>
                                    )}
                                </Stack>
                            </Box>
                        </Stack>
                    </Paper>

                    <Paper sx={{p: {xs: 3, md: 4}, borderRadius: '2rem'}}>
                        <Stack spacing={2}>
                            <Box>
                                <Typography variant="h4" fontWeight={900}>My submissions</Typography>
                                <Typography color="text.secondary">
                                    Track what is pending, approved, rejected, or withdrawn.
                                </Typography>
                            </Box>

                            {submissions.length === 0 ? (
                                <Typography color="text.secondary">
                                    You have not submitted any games yet.
                                </Typography>
                            ) : (
                                <Stack spacing={2}>
                                    {submissions.map(submission => (
                                        <Paper key={submission.id} variant="outlined" sx={{p: 2.5, borderRadius: '1.25rem'}}>
                                            <Stack spacing={1.5}>
                                                <Stack direction={{xs: 'column', sm: 'row'}} justifyContent="space-between" spacing={1}>
                                                    <Box>
                                                        <Typography variant="h6" fontWeight={800}>{submission.name}</Typography>
                                                        <Typography color="text.secondary">
                                                            {submission.platforms || 'Platform not provided'}
                                                        </Typography>
                                                    </Box>
                                                    <Chip label={submission.status || 'PENDING'} color={statusChipColor(submission.status)}/>
                                                </Stack>
                                                {submission.description && (
                                                    <Typography color="text.secondary">{submission.description}</Typography>
                                                )}
                                                <Typography variant="body2" color="text.secondary">
                                                    Submitted {formatDateTime(submission.createdAt)}
                                                </Typography>
                                                {submission.reviewNotes && (
                                                    <Typography variant="body2" color="text.secondary">
                                                        Admin note: {submission.reviewNotes}
                                                    </Typography>
                                                )}
                                                {submission.createdGameId && (
                                                    <Typography variant="body2" color="text.secondary">
                                                        Approved into catalog as {submission.createdGameName || submission.createdGameId}.
                                                    </Typography>
                                                )}
                                                {(submission.canEdit || submission.canWithdraw) && (
                                                    <Stack direction={{xs: 'column', sm: 'row'}} spacing={1.25}>
                                                        {submission.canEdit && (
                                                            <Button
                                                                variant="outlined"
                                                                onClick={() => beginEditingSubmission(submission)}
                                                            >
                                                                {submission.status === 'REJECTED' || submission.status === 'WITHDRAWN'
                                                                    ? 'Edit and resubmit'
                                                                    : 'Edit request'}
                                                            </Button>
                                                        )}
                                                        {submission.canWithdraw && (
                                                            <Button
                                                                variant="text"
                                                                color="inherit"
                                                                onClick={() => handleWithdrawSubmission(submission.id)}
                                                            >
                                                                Withdraw request
                                                            </Button>
                                                        )}
                                                    </Stack>
                                                )}
                                            </Stack>
                                        </Paper>
                                    ))}
                                </Stack>
                            )}
                        </Stack>
                    </Paper>
                </Stack>
            </Container>
        </Box>
    );
}

export default SubmitGamePage;
